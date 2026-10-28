/**
 * packageName    : io.miraclecat.fbs.api.flight.dto
 * fileName       : FlightInventoryDto
 * author         : MiracleCat
 * date           : 2026-05-19
 * description    : 항공편 재고 DTO
 */
package io.miraclecat.fbs.api.flight.dto;

import lombok.*;

import java.time.LocalDate;

public class FlightInventoryDto {

    /**
     * 재고 목록 응답 (관리자 항공편 상세)
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchRes {
        private Long id;
        private Long flightId;
        private String flightNumber;
        private LocalDate flightDate;
        private Integer availableSeats;
        private Integer version;
        private Long bookedSeats;   // 확정 예약 좌석 수 (seat_reservations)
    }

    /**
     * 재고 상세 조회 응답
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DetailRes {
        private Long id;
        private Long flightId;
        private String flightNumber;
        private LocalDate flightDate;
        private Integer availableSeats;
    }

    /**
     * 재고 수동 조정 요청
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdjustReq {
        private Integer availableSeats;
    }

    /**
     * 재고 일괄 생성 요청
     */
    @Getter
    @Setter
    public static class GenerateReq {
        private LocalDate startDate;
        private LocalDate endDate;
    }

    /**
     * 재고 일괄 생성 응답
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GenerateRes {
        private int createdCount;
        private int skippedCount;
    }

}
