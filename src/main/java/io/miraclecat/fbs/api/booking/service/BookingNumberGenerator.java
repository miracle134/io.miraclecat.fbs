/**
 * packageName    : io.miraclecat.fbs.api.booking.service
 * fileName       : BookingNumberGenerator
 * author         : MiracleCat
 * date           : 2026-05-19
 * description    : 예약번호 채번 (Redis INCR 기반)
 *                  - Redis Key: booking:seq:{yyyyMMdd}, TTL: 25시간
 *                  - INCR은 원자적이라 다중 서버·재시작에도 중복 없음
 */
package io.miraclecat.fbs.api.booking.service;

import io.miraclecat.fbs.core.utils.data.RedisUtil;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
public class BookingNumberGenerator {

    private static final String KEY_PREFIX = "booking:seq:";
    private static final long TTL_SECONDS = Duration.ofHours(25).toSeconds();
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * 예약번호 생성
     * 형식: BK{yyyyMMdd}{6자리 시퀀스}
     * 예시: BK20260519000001 (16자리)
     */
    public String generate() {
        String today = LocalDate.now().format(DATE_FORMATTER);
        String key = KEY_PREFIX + today;

        long sequence = RedisUtil.increment(key);
        if (sequence == 1L) {
            RedisUtil.expire(key, TTL_SECONDS);
        }
        return "BK" + today + String.format("%06d", sequence);
    }

}
