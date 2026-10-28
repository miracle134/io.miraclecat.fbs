/**
 * packageName    : io.miraclecat.fbs.api.booking.dto
 * fileName       : BookingHistoryDto
 * author         : MiracleCat
 * date           : 2026-05-19
 * description    : 예약 이력 DTO
 */
package io.miraclecat.fbs.api.booking.dto;

import lombok.*;

import java.time.LocalDateTime;

public class BookingHistoryDto {

    /**
     * 예약 이력 목록 조회 요청
     */
    @Getter
    @Setter
    public static class SearchReq {
        private Long bookingId;
        private String changedBy;
    }

    /**
     * 예약 이력 목록 조회 응답
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchRes {
        private Long id;
        private Long bookingId;
        private String bookingNumber;
        private String previousStatus;
        private String currentStatus;
        private String changedBy;
        private String reason;
        private LocalDateTime createdAt;
    }

}
