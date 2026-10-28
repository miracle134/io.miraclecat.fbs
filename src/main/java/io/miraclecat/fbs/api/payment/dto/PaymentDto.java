/**
 * packageName    : io.miraclecat.fbs.api.payment.dto
 * fileName       : PaymentDto
 * author         : MiracleCat
 * date           : 2026-05-19
 * description    : 결제 DTO
 */
package io.miraclecat.fbs.api.payment.dto;

import lombok.*;

import java.util.List;

public class PaymentDto {

    /**
     * 결제 준비 요청 (결제하기 클릭)
     */
    @Getter
    @Setter
    public static class PrepareReq {
        private String sessionId;
        private List<PassengerReq> passengers;
    }

    /**
     * 결제 준비 요청 - 탑승객
     */
    @Getter
    @Setter
    public static class PassengerReq {
        private String name;
        private String nameEn;
        private String email;
        private String mobile;
        private String birthDate;
        private String gender;
        private String seatNumber;
    }

    /**
     * 결제 준비 응답 (토스 결제창 호출 값)
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PrepareRes {
        private String orderId;
        private String orderName;
        private Integer amount;
        private String customerName;
        private String customerEmail;
        private Long expireAt;
        private Long serverTime;
    }

    /**
     * 결제 승인 요청 (토스 successUrl 복귀 값 + 예약 세션)
     * - orderId = 예약번호
     * - amount는 세션에 저장한 서버 계산 금액과 비교(위변조 검증)
     */
    @Getter
    @Setter
    public static class ConfirmReq {
        private String sessionId;
        private String paymentKey;
        private String orderId;
        private Integer amount;
        private String clientId;
    }

    /**
     * 결제 승인 응답
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConfirmRes {
        private Long bookingId;
        private String bookingNumber;
    }

}
