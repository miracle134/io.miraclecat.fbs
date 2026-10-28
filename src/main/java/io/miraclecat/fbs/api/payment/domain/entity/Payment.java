/**
 * packageName    : io.miraclecat.fbs.api.payment.domain.entity
 * fileName       : Payment
 * author         : MiracleCat
 * date           : 2026-05-19
 * description    : 결제 엔티티
 *                  - 토스 승인 완료 후 저장 → 생성 시 바로 APPROVED
 *                  - 부분/전액 취소 모두 CANCELLED, refund_amount로 구분
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 2026-05-19        MiracleCat         최초 생성
 * 2026-10-25        MiracleCat         승인 후 생성 구조로 변경, 환불 금액 추가
 */
package io.miraclecat.fbs.api.payment.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "payments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {

    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_id", nullable = false)
    private Long bookingId;

    @Column(name = "payment_key", length = 100)
    private String paymentKey;

    @Column(name = "amount", nullable = false)
    private Integer amount;

    @Column(name = "refund_amount")
    private Integer refundAmount;

    @Column(name = "method", nullable = false, length = 20)
    private String method;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancel_reason", columnDefinition = "TEXT")
    private String cancelReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * @param approvedAt 토스 승인 응답의 승인 시각 (없으면 현재 시각)
     */
    @Builder
    public Payment(Long bookingId, String paymentKey, Integer amount, String method, LocalDateTime approvedAt) {
        this.bookingId = bookingId;
        this.paymentKey = paymentKey;
        this.amount = amount;
        this.method = method;
        this.status = STATUS_APPROVED;
        this.approvedAt = approvedAt != null ? approvedAt : LocalDateTime.now();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 결제 취소
     * @param refundAmount 사용자 취소: fare_amount / 관리자 전액 취소: total_amount
     */
    public void cancelPayment(Integer refundAmount, String cancelReason) {
        this.status = STATUS_CANCELLED;
        this.refundAmount = refundAmount;
        this.cancelReason = cancelReason;
        this.cancelledAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

}