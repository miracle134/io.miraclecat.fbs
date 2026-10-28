/**
 * packageName    : io.miraclecat.fbs.api.booking.dto
 * fileName       : BookingSessionDto
 * author         : MiracleCat
 * date           : 2026-10-25
 * description    : 예약 세션 DTO (검색 → 예약 화면 → 결제까지 유지되는 서버 세션)
 */
package io.miraclecat.fbs.api.booking.dto;

import lombok.*;

import java.util.List;

public class BookingSessionDto {

    /**
     * 예약 세션 생성 요청
     */
    @Getter
    @Setter
    public static class CreateReq {
        private Long inventoryId;
        private Integer passengerCount;
    }

    /**
     * Redis 저장용 세션 (시각은 epoch ms)
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Session {
        private String sessionId;
        private Long memberId;
        private Long flightId;
        private Long inventoryId;
        private String flightDate;
        private String departureAirport;
        private String arrivalAirport;
        private Integer passengerCount;
        private Integer basePrice;
        private Integer fareAmount;
        private Integer bookingFeeAmount;
        private Integer totalAmount;
        private Long createdAt;
        private Long expireAt;
        private Long maxExpireAt;
        private String orderId;
        private List<Passenger> passengers;
    }

    /**
     * 결제하기 시점에 세션에 저장되는 탑승객 정보
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Passenger {
        private String name;
        private String nameEn;
        private String email;
        private String mobile;
        private String birthDate;
        private String gender;
        private String seatNumber;
    }

    /**
     * 예약 세션 응답
     * serverTime : 클라이언트 시계 오차 보정용
     * listUrl    : 만료 시 이동할 같은 조건의 항공편 목록
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Res {
        private String sessionId;
        private Long flightId;
        private Long inventoryId;
        private String flightDate;
        private String departureAirport;
        private String arrivalAirport;
        private Integer passengerCount;
        private Integer basePrice;
        private Integer fareAmount;
        private Integer bookingFeeAmount;
        private Integer totalAmount;
        private Long expireAt;
        private Long serverTime;
        private String listUrl;
    }

}
