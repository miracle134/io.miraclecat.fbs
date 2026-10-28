/**
 * packageName    : io.miraclecat.fbs.pg.toss
 * fileName       : TossPaymentDto
 * author         : MiracleCat
 * date           : 2026-05-19
 * description    : 토스페이먼츠 요청/응답 DTO
 */
package io.miraclecat.fbs.pg.toss;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

public class TossPaymentDto {

    /**
     * 토스 결제 승인 요청
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConfirmReq {
        private String paymentKey;
        private String orderId;
        private Integer amount;
    }

    /**
     * 토스 결제 승인 응답
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConfirmRes {
        private String paymentKey;
        private String orderId;
        private String status;
        private String method;
        private Integer totalAmount;
        private String approvedAt;
    }

    /**
     * 토스 결제 취소 요청
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CancelReq {
        private String cancelReason;
        private Integer cancelAmount;
    }

    /**
     * 토스 결제 취소 응답
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CancelRes {
        private String paymentKey;
        private String status;
        private String cancelReason;
    }

    /**
     * 토스 오류 응답
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorRes {
        private String code;
        private String message;
    }

}
