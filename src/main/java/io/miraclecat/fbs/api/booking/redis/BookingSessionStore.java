/**
 * packageName    : io.miraclecat.fbs.api.booking.redis
 * fileName       : BookingSessionStore
 * author         : MiracleCat
 * date           : 2026-10-25
 * description    : 예약 세션 Redis 저장소
 *
 * 키 구조
 *   booking:session:{sessionId}  String(JSON)  TTL = 만료 시각까지 남은 시간
 */
package io.miraclecat.fbs.api.booking.redis;

import io.miraclecat.fbs.api.booking.dto.BookingSessionDto;
import io.miraclecat.fbs.core.utils.data.RedisUtil;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class BookingSessionStore {

    private static final String KEY_PREFIX = "booking:session:";

    /**
     * 저장 (TTL은 세션 만료 시각 기준)
     */
    public void save(BookingSessionDto.Session session) {
        long remainMs = session.getExpireAt() - System.currentTimeMillis();
        long ttlSeconds = Math.max(1, (remainMs + 999) / 1000);
        RedisUtil.setObject(key(session.getSessionId()), session, ttlSeconds);
    }

    public Optional<BookingSessionDto.Session> find(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) return Optional.empty();
        return Optional.ofNullable(RedisUtil.getObject(key(sessionId), BookingSessionDto.Session.class));
    }

    public void delete(String sessionId) {
        RedisUtil.delete(key(sessionId));
    }

    private String key(String sessionId) {
        return KEY_PREFIX + sessionId;
    }

}
