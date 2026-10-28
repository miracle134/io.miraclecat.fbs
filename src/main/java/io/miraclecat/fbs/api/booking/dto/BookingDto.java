/**
 * packageName    : io.miraclecat.fbs.api.booking.dto
 * fileName       : BookingDto
 * author         : MiracleCat
 * date           : 2026-05-19
 * description    : 예약 DTO
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 2026-05-19        MiracleCat         최초 생성
 */
package io.miraclecat.fbs.api.booking.dto;

import io.miraclecat.fbs.core.utils.persistence.PageReq;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public class BookingDto {

    /**
     * 예약 목록 조회 요청 (관리자)
     * dateType : created(예약일, 기본) / flight(출발일)
     */
    @Getter
    @Setter
    public static class AdminSearchReq extends PageReq {
        private String bookingNumber;
        private String bookerName;
        private String dateType;
        @DateTimeFormat(pattern = "yyyy-MM-dd")
        private LocalDate startDate;
        @DateTimeFormat(pattern = "yyyy-MM-dd")
        private LocalDate endDate;
        private String status;
    }

    /**
     * 예약 목록 조회 응답 (관리자)
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdminSearchRes {
        private Long id;
        private String bookingNumber;
        private Long memberId;
        private String memberName;
        private String loginId;
        private String flightNumber;
        private String departureAirport;
        private String departureAirportNameKo;
        private String arrivalAirport;
        private String arrivalAirportNameKo;
        private LocalDate flightDate;
        private Integer passengerCount;
        private Integer totalAmount;
        private String status;
        private LocalDateTime createdAt;
    }

    /**
     * 예약 상세 조회 응답 (관리자, 탑승객·이력 포함)
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdminDetailRes {
        private Long id;
        private String bookingNumber;
        private String status;
        private LocalDate flightDate;
        private Integer passengerCount;
        private Integer fareAmount;
        private Integer bookingFeeAmount;
        private Integer totalAmount;
        private String cancelledBy;
        private String cancelReason;
        private LocalDateTime cancelledAt;
        private LocalDateTime createdAt;
        // 예약자
        private Long memberId;
        private String memberName;
        private String loginId;
        private String memberMobile;
        // 항공편
        private String flightNumber;
        private String airlineNameKo;
        private String departureAirport;
        private String departureAirportNameKo;
        private String arrivalAirport;
        private String arrivalAirportNameKo;
        private LocalTime departureTime;
        private LocalTime arrivalTime;
        // 결제
        private String paymentMethod;
        private String paymentStatus;
        private Integer paymentAmount;
        private Integer refundAmount;
        private LocalDateTime approvedAt;
        private LocalDateTime paymentCancelledAt;
        private List<AdminPassenger> passengers;
        private List<BookingHistoryDto.SearchRes> histories;
    }

    /**
     * 예약 상세 - 탑승객 (관리자)
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdminPassenger {
        private Long id;
        private String name;
        private String nameEn;
        private String gender;
        private LocalDate birthDate;
        private String email;
        private String mobile;
        private String seatNumber;
    }

    /**
     * 관리자 예약 취소 요청
     * refundType : FULL(전액 — 예약 수수료 포함) / FARE(항공권 금액만)
     */
    @Getter
    @Setter
    public static class AdminCancelReq {
        private String refundType;
        private String cancelReason;
    }

    /**
     * 예약 취소 요청 (사용자)
     * cancelledBy는 요청으로 받지 않음 — 사용자/관리자 취소 서비스에서 지정
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CancelReq {
        private String cancelReason;
    }

    /**
     * 내 예약 목록 조회 요청
     * filter : all / upcoming(예정) / completed(완료) / cancelled(취소)
     */
    @Getter
    @Setter
    public static class MyReq extends PageReq {
        private String filter;
    }

    /**
     * 내 예약 목록 조회 응답
     * changeable : 좌석 변경·취소 가능 여부 (확정 + 출발 하루 전까지)
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MyRes {
        private Long id;
        private String bookingNumber;
        private String status;
        private LocalDate flightDate;
        private Integer passengerCount;
        private Integer fareAmount;
        private Integer bookingFeeAmount;
        private Integer totalAmount;
        private LocalDateTime createdAt;
        private LocalDateTime cancelledAt;
        private Long flightId;
        private Long inventoryId;
        private String flightNumber;
        private String airlineNameKo;
        private String departureAirport;
        private String departureAirportNameKo;
        private String arrivalAirport;
        private String arrivalAirportNameKo;
        private LocalTime departureTime;
        private LocalTime arrivalTime;
        private Integer durationMinutes;
        private Boolean changeable;
        private List<MyPassenger> passengers;
    }

    /**
     * 내 예약 목록 - 탑승객
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MyPassenger {
        private Long id;
        private Long bookingId;
        private String name;
        private String seatNumber;
    }

    /**
     * 좌석 변경 요청
     */
    @Getter
    @Setter
    public static class SeatChangeReq {
        private String clientId;
        private List<SeatChangeItem> changes;
    }

    /**
     * 좌석 변경 요청 - 탑승객별 새 좌석
     */
    @Getter
    @Setter
    public static class SeatChangeItem {
        private Long passengerId;
        private String seatNumber;
    }

}