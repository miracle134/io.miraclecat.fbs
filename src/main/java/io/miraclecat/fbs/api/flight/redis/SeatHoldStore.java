/**
 * packageName    : io.miraclecat.fbs.api.flight.redis
 * fileName       : SeatHoldStore
 * author         : MiracleCat
 * date           : 2026-10-25
 * description    : 좌석 임시 선점 Redis 저장소
 *
 * 키 구조
 *   seat:hold:{inventoryId}:{seatNo}  String  값: 선점 회원 ID, TTL — 좌석 소유자 / 원자적 선점
 *   seat:holds:{inventoryId}          ZSET    멤버: 좌석번호, 점수: 만료시각(ms) — 재고별 선점 목록 / 만료 정리
 *   seat:hold:inventories             SET     선점이 존재하는 재고 ID 목록 — 만료 스케줄러 순회용
 *
 * 선점/해제는 Lua 스크립트로 처리 — "확인 후 저장/삭제"를 Redis 안에서 한 번에 실행해 원자성 보장
 */
package io.miraclecat.fbs.api.flight.redis;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SeatHoldStore {

    private static final String SEAT_KEY_PREFIX = "seat:hold:";
    private static final String HOLDS_KEY_PREFIX = "seat:holds:";
    static final String INVENTORIES_KEY = "seat:hold:inventories";

    /**
     * 선점: 비어있거나 본인 소유면 저장(TTL 갱신), 다른 사람 소유면 실패
     * KEYS: seatKey, holdsKey, inventoriesKey / ARGV: memberId, ttlMs, expireAtMs, seatNo, inventoryId
     * return: 0 실패, 1 신규 선점, 2 본인 선점 연장
     */
    private static final RedisScript<Long> HOLD_SCRIPT = new DefaultRedisScript<>("""
            local current = redis.call('GET', KEYS[1])
            if current and current ~= ARGV[1] then
                return 0
            end
            redis.call('SET', KEYS[1], ARGV[1], 'PX', ARGV[2])
            redis.call('ZADD', KEYS[2], ARGV[3], ARGV[4])
            redis.call('SADD', KEYS[3], ARGV[5])
            if current then
                return 2
            end
            return 1
            """, Long.class);

    /**
     * 해제: 본인 소유일 때만 삭제
     * KEYS: seatKey, holdsKey / ARGV: memberId, seatNo
     * return: 0 실패(본인 소유 아님/없음), 1 해제
     */
    private static final RedisScript<Long> RELEASE_SCRIPT = new DefaultRedisScript<>("""
            if redis.call('GET', KEYS[1]) ~= ARGV[1] then
                return 0
            end
            redis.call('DEL', KEYS[1])
            redis.call('ZREM', KEYS[2], ARGV[2])
            return 1
            """, Long.class);

    /**
     * 만료 정리: 만료 시각이 지났고 좌석 키도 사라진 좌석만 목록에서 제거, 재고에 선점이 없으면 재고 목록에서도 제거
     * KEYS: holdsKey, inventoriesKey / ARGV: now(ms), seatKeyPrefix, inventoryId
     * return: 해제된 좌석 번호 목록
     */
    @SuppressWarnings("rawtypes")
    private static final RedisScript<List> RELEASE_EXPIRED_SCRIPT = new DefaultRedisScript<>("""
            local seats = redis.call('ZRANGEBYSCORE', KEYS[1], '-inf', ARGV[1])
            local released = {}
            for _, seat in ipairs(seats) do
                if redis.call('EXISTS', ARGV[2] .. seat) == 0 then
                    redis.call('ZREM', KEYS[1], seat)
                    table.insert(released, seat)
                end
            end
            if redis.call('ZCARD', KEYS[1]) == 0 then
                redis.call('SREM', KEYS[2], ARGV[3])
            end
            return released
            """, List.class);

    public enum HoldResult {
        HELD, EXTENDED, FAILED
    }

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * -- GETTER --
     *  선점 만료 시각(ms) 계산용 TTL
     */
    @Getter
    @Value("${seat.hold.ttl-seconds:300}")
    private long ttlSeconds;

    /**
     * 좌석 선점
     */
    public HoldResult hold(Long inventoryId, String seatNo, Long memberId) {
        return hold(inventoryId, seatNo, memberId, ttlSeconds * 1000);
    }

    /**
     * 좌석 선점 (TTL 지정)
     */
    public HoldResult hold(Long inventoryId, String seatNo, Long memberId, long ttlMs) {
        long expireAt = System.currentTimeMillis() + ttlMs;

        Long result = stringRedisTemplate.execute(HOLD_SCRIPT,
                List.of(seatKey(inventoryId, seatNo), holdsKey(inventoryId), INVENTORIES_KEY),
                String.valueOf(memberId), String.valueOf(ttlMs), String.valueOf(expireAt),
                seatNo, String.valueOf(inventoryId));

        if (result == null || result == 0L) return HoldResult.FAILED;
        return result == 2L ? HoldResult.EXTENDED : HoldResult.HELD;
    }

    /**
     * 회원의 유효한 선점 전체 연장
     * @return 연장된 좌석 번호 목록
     */
    public List<String> extendAll(Long inventoryId, Long memberId) {
        return extendAll(inventoryId, memberId, ttlSeconds * 1000);
    }

    /**
     * 회원의 유효한 선점 전체 연장 (TTL 지정 — 결제 진행 중에는 예약 세션 만료 시각까지)
     * @return 연장된 좌석 번호 목록
     */
    public List<String> extendAll(Long inventoryId, Long memberId, long ttlMs) {
        String me = String.valueOf(memberId);
        List<String> extended = new ArrayList<>();
        findActiveHolds(inventoryId).forEach((seatNo, holder) -> {
            if (me.equals(holder) && hold(inventoryId, seatNo, memberId, ttlMs) != HoldResult.FAILED) {
                extended.add(seatNo);
            }
        });
        return extended;
    }

    /**
     * 좌석 선점 해제 (본인 소유일 때만)
     */
    public boolean release(Long inventoryId, String seatNo, Long memberId) {
        Long result = stringRedisTemplate.execute(RELEASE_SCRIPT,
                List.of(seatKey(inventoryId, seatNo), holdsKey(inventoryId)),
                String.valueOf(memberId), seatNo);
        return result != null && result == 1L;
    }

    /**
     * 선점이 존재하는 재고 ID 목록
     */
    public Set<Long> findHoldInventoryIds() {
        Set<String> members = stringRedisTemplate.opsForSet().members(INVENTORIES_KEY);
        if (members == null || members.isEmpty()) {
            return Set.of();
        }
        return members.stream().map(Long::valueOf).collect(Collectors.toSet());
    }

    /**
     * 재고의 만료된 선점 정리
     * @return 해제된 좌석 번호 목록
     */
    @SuppressWarnings("unchecked")
    public List<String> releaseExpired(Long inventoryId) {
        List<String> released = stringRedisTemplate.execute(RELEASE_EXPIRED_SCRIPT,
                List.of(holdsKey(inventoryId), INVENTORIES_KEY),
                String.valueOf(System.currentTimeMillis()),
                SEAT_KEY_PREFIX + inventoryId + ":",
                String.valueOf(inventoryId));
        return released != null ? released : List.of();
    }

    /**
     * 재고의 유효한 선점 좌석 조회
     * @return { 좌석번호: 선점 회원 ID }
     */
    public Map<String, String> findActiveHolds(Long inventoryId) {
        long now = System.currentTimeMillis();
        Set<String> seats = stringRedisTemplate.opsForZSet()
                .rangeByScore(holdsKey(inventoryId), now, Double.POSITIVE_INFINITY);
        if (seats == null || seats.isEmpty()) {
            return Map.of();
        }

        List<String> seatList = new ArrayList<>(seats);
        List<String> holders = stringRedisTemplate.opsForValue()
                .multiGet(seatList.stream().map(seat -> seatKey(inventoryId, seat)).toList());

        Map<String, String> result = new LinkedHashMap<>();
        for (int i = 0; i < seatList.size(); i++) {
            String holder = holders != null ? holders.get(i) : null;
            if (holder != null) {
                result.put(seatList.get(i), holder);
            }
        }
        return result;
    }

    /**
     * 회원의 유효한 선점 좌석 수
     */
    public long countHoldsByMember(Long inventoryId, Long memberId) {
        String me = String.valueOf(memberId);
        return findActiveHolds(inventoryId).values().stream().filter(me::equals).count();
    }

    static String seatKey(Long inventoryId, String seatNo) {
        return SEAT_KEY_PREFIX + inventoryId + ":" + seatNo;
    }

    static String holdsKey(Long inventoryId) {
        return HOLDS_KEY_PREFIX + inventoryId;
    }

}
