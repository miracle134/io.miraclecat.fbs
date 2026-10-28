/**
 * packageName    : io.miraclecat.fbs.api.booking.domain.entity
 * fileName       : Booking
 * author         : MiracleCat
 * date           : 2026-05-19
 * description    : 예약 엔티티
 *                  - 결제 승인 후 일괄 저장 → 생성 시 바로 confirmed
 *                  - total_amount = fare_amount(환불 기준) + booking_fee_amount(환불 불가)
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 2026-05-19        MiracleCat         최초 생성
 * 2026-10-25        MiracleCat         금액 분리, 미결제 만료(pending/expire_at) 제거
 */
package io.miraclecat.fbs.api.booking.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "bookings")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Booking {

    public static final String STATUS_CONFIRMED = "confirmed";
    public static final String STATUS_CANCELLED = "cancelled";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_number", nullable = false, unique = true, length = 20)
    private String bookingNumber;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "flight_id", nullable = false)
    private Long flightId;

    @Column(name = "flight_date", nullable = false)
    private LocalDate flightDate;

    @Column(name = "passenger_count", nullable = false)
    private Integer passengerCount;

    @Column(name = "fare_amount", nullable = false)
    private Integer fareAmount;

    @Column(name = "booking_fee_amount", nullable = false)
    private Integer bookingFeeAmount;

    @Column(name = "total_amount", nullable = false)
    private Integer totalAmount;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "payment_method", length = 20)
    private String paymentMethod;

    @Column(name = "cancelled_by", length = 20)
    private String cancelledBy;

    @Column(name = "cancel_reason", columnDefinition = "TEXT")
    private String cancelReason;

    @Column(name = "created_id")
    private Long createdId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_id")
    private Long updatedId;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    /**
     * total_amount는 받지 않고 fare + fee로 계산 (DB CHECK 제약과 항상 일치)
     */
    @Builder
    public Booking(String bookingNumber, Long memberId, Long flightId, LocalDate flightDate,
                   Integer passengerCount, Integer fareAmount, Integer bookingFeeAmount,
                   String paymentMethod, Long createdId) {
        this.bookingNumber = bookingNumber;
        this.memberId = memberId;
        this.flightId = flightId;
        this.flightDate = flightDate;
        this.passengerCount = passengerCount;
        this.fareAmount = fareAmount;
        this.bookingFeeAmount = bookingFeeAmount;
        this.totalAmount = fareAmount + bookingFeeAmount;
        this.status = STATUS_CONFIRMED;
        this.paymentMethod = paymentMethod;
        this.createdId = createdId;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 예약 취소
     * @param cancelledBy USER / ADMIN — 요청값이 아니라 서비스(사용자/관리자 취소)에서 지정
     */
    public void cancelBooking(String cancelledBy, String cancelReason, Long updatedId) {
        this.status = STATUS_CANCELLED;
        this.cancelledBy = cancelledBy;
        this.cancelReason = cancelReason;
        this.cancelledAt = LocalDateTime.now();
        this.updatedId = updatedId;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 좌석 변경 등 상태 변화 없는 수정 시 수정자/수정일 갱신
     */
    public void touch(Long updatedId) {
        this.updatedId = updatedId;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isConfirmed() {
        return STATUS_CONFIRMED.equals(this.status);
    }

    /**
     * 출발 하루 전까지(오늘 < 출발일)만 사용자 변경/취소 가능
     */
    public boolean isChangeableBy(LocalDate today) {
        return today.isBefore(this.flightDate);
    }

    public boolean isOwnedBy(Long memberId) {
        return this.memberId.equals(memberId);
    }

}