/**
 * packageName    : io.miraclecat.fbs.api.flight.dto
 * fileName       : FlightDto
 * author         : MiracleCat
 * date           : 2026-05-19
 * description    : 항공편 DTO
 */
package io.miraclecat.fbs.api.flight.dto;

import io.miraclecat.fbs.core.utils.persistence.PageReq;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public class FlightDto {

    /**
     * 항공편 목록 조회 요청
     */
    @Getter
    @Setter
    public static class SearchReq extends PageReq {
        private String flightNumber;
        private String airlineCode;
        private List<String> airlineCodes;
        private String departureAirport;
        private String arrivalAirport;
        private LocalDate flightDate;
        private Integer minPrice;
        private Integer maxPrice;
        private Integer passengers;
        private List<String> departureTimeSlots;
        private String status;
        private String sortType; // PRICE_ASC, DEPARTURE_ASC, DURATION_ASC
    }

    /**
     * 항공편 목록 조회 응답
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchRes {
        private Long id;
        private Long flightInventoryId;
        private String flightNumber;
        private String airlineCode;
        private String airlineNameKo;
        private String logoUrl;
        private String departureAirport;
        private String departureAirportNameKo;
        private String arrivalAirport;
        private String arrivalAirportNameKo;
        private LocalDate flightDate;
        private LocalTime departureTime;
        private LocalTime arrivalTime;
        private Integer durationMinutes;
        private Integer totalSeats;
        private Integer basePrice;
        private Integer availableSeats;
        private String status;
    }

    /**
     * 항공편 상세 조회 응답
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DetailRes {
        private Long id;
        private String flightNumber;
        private String airlineCode;
        private String airlineNameKo;
        private String logoUrl;
        private String departureAirport;
        private String departureAirportNameKo;
        private String arrivalAirport;
        private String arrivalAirportNameKo;
        private LocalTime departureTime;
        private LocalTime arrivalTime;
        private Integer durationMinutes;
        private Integer totalSeats;
        private Integer basePrice;
        private String status;
    }

    /**
     * 항공편 상세 응답 (관리자, 재고 현황 포함)
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdminDetailRes {
        private Long id;
        private String flightNumber;
        private String airlineCode;
        private String airlineNameKo;
        private String logoUrl;
        private String departureAirport;
        private String departureAirportNameKo;
        private String arrivalAirport;
        private String arrivalAirportNameKo;
        private LocalTime departureTime;
        private LocalTime arrivalTime;
        private Integer durationMinutes;
        private Integer totalSeats;
        private Integer basePrice;
        private String status;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private LocalDate lastInventoryDate;      // 생성된 재고의 마지막 날짜
        private Long upcomingInventoryCount;      // 오늘 이후 재고 수
        private List<FlightInventoryDto.SearchRes> upcomingInventories;
    }

    /**
     * 항공편 등록 요청
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateReq {
        private String flightNumber;
        private String airlineCode;
        private String departureAirport;
        private String arrivalAirport;
        private LocalTime departureTime;
        private LocalTime arrivalTime;
        private Integer durationMinutes;
        private Integer totalSeats;
        private Integer basePrice;
        private String status;
    }

    /**
     * 항공편 수정 요청
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateReq {
        private String airlineCode;
        private String departureAirport;
        private String arrivalAirport;
        private LocalTime departureTime;
        private LocalTime arrivalTime;
        private Integer durationMinutes;
        private Integer totalSeats;
        private Integer basePrice;
        private String status;
    }

}
