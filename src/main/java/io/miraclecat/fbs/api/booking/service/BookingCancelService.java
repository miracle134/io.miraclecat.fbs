/**
 * packageName    : io.miraclecat.fbs.api.booking.service
 * fileName       : BookingCancelService
 * author         : MiracleCat
 * date           : 2026-10-25
 * description    : 예약 취소 흐름 (트랜잭션 없음)
 *                  - 사전 검증 → 토스 부분 취소(트랜잭션 밖, 멱등키) → BookingCancelTxService → 커밋 후 RELEASED 전파
 *                  - 사용자 취소: 출발 하루 전까지, 항공권 금액만 환불 (예약 수수료 환불 불가)
 *                  - 관리자 취소: 기한 없음, 전액(FULL) 또는 항공권 금액만(FARE) 선택
 */
package io.miraclecat.fbs.api.booking.service;

import io.miraclecat.fbs.api.booking.domain.entity.Booking;
import io.miraclecat.fbs.api.booking.domain.repository.BookingRepository;
import io.miraclecat.fbs.api.booking.dto.BookingDto;
import io.miraclecat.fbs.api.flight.domain.repository.FlightInventoryRepository;
import io.miraclecat.fbs.api.flight.dto.SeatDto;
import io.miraclecat.fbs.api.flight.websocket.SeatEventPublisher;
import io.miraclecat.fbs.api.payment.domain.entity.Payment;
import io.miraclecat.fbs.api.payment.domain.repository.PaymentRepository;
import io.miraclecat.fbs.core.exception.BusinessException;
import io.miraclecat.fbs.core.response.ApiEntity;
import io.miraclecat.fbs.core.response.HttpsStatusCode;
import io.miraclecat.fbs.core.utils.security.SecurityUtil;
import io.miraclecat.fbs.pg.toss.TossApiException;
import io.miraclecat.fbs.pg.toss.TossPaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

import static io.miraclecat.fbs.core.exception.GlobalException.business;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingCancelService {

    private static final String CANCELLED_BY_USER = "USER";
    private static final String CANCELLED_BY_ADMIN = "ADMIN";
    private static final String DEFAULT_REASON = "고객 요청 취소";
    private static final String DEFAULT_ADMIN_REASON = "관리자 취소";
    private static final String REFUND_FULL = "FULL";
    private static final String REFUND_FARE = "FARE";
    private static final String ALREADY_CANCELED = "ALREADY_CANCELED_PAYMENT";

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final FlightInventoryRepository flightInventoryRepository;
    private final TossPaymentService tossPaymentService;
    private final BookingCancelTxService bookingCancelTxService;
    private final SeatEventPublisher seatEventPublisher;

    /**
     * 사용자 예약 취소
     */
    public ApiEntity<Void> cancelByUser(Long bookingId, BookingDto.CancelReq req) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        if (memberId == null) {
            throw business(HttpsStatusCode.LOGIN_REQUIRED);
        }

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> business(HttpsStatusCode.RESOURCE_NOT_FOUND));
        if (!booking.isOwnedBy(memberId)) {
            throw business(HttpsStatusCode.ACCESS_DENIED);
        }
        if (!booking.isConfirmed()) {
            throw business("이미 취소된 예약입니다.", HttpsStatusCode.UNPROCESSABLE_ENTITY);
        }
        if (!booking.isChangeableBy(LocalDate.now())) {
            throw business("출발 하루 전까지만 취소할 수 있습니다.", HttpsStatusCode.UNPROCESSABLE_ENTITY);
        }

        String reason = req != null && req.getCancelReason() != null && !req.getCancelReason().isBlank()
                ? req.getCancelReason().trim() : DEFAULT_REASON;
        int refundAmount = booking.getFareAmount();

        cancel(booking, memberId, CANCELLED_BY_USER, reason, refundAmount, booking.getBookingNumber() + "-cancel");
        return ApiEntity.of(HttpsStatusCode.SUCCESS);
    }

    /**
     * 관리자 예약 취소 (기한 제한 없음)
     * FULL: 결제 금액 전액(예약 수수료 포함) / FARE: 항공권 금액만
     */
    public ApiEntity<Void> cancelByAdmin(Long bookingId, BookingDto.AdminCancelReq req) {
        Long adminId = SecurityUtil.getCurrentMemberId();
        if (adminId == null) {
            throw business(HttpsStatusCode.LOGIN_REQUIRED);
        }
        String refundType = req != null ? req.getRefundType() : null;
        if (!REFUND_FULL.equals(refundType) && !REFUND_FARE.equals(refundType)) {
            throw business("환불 방식을 선택해주세요.", HttpsStatusCode.INVALID_PARAMETER);
        }

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> business(HttpsStatusCode.RESOURCE_NOT_FOUND));
        if (!booking.isConfirmed()) {
            throw business("이미 취소된 예약입니다.", HttpsStatusCode.UNPROCESSABLE_ENTITY);
        }

        String reason = req.getCancelReason() != null && !req.getCancelReason().isBlank()
                ? req.getCancelReason().trim() : DEFAULT_ADMIN_REASON;
        int refundAmount = REFUND_FULL.equals(refundType) ? booking.getTotalAmount() : booking.getFareAmount();

        cancel(booking, adminId, CANCELLED_BY_ADMIN, reason, refundAmount,
                booking.getBookingNumber() + "-cancel-" + refundType.toLowerCase());
        return ApiEntity.of(HttpsStatusCode.SUCCESS);
    }

    /**
     * 토스 취소(트랜잭션 밖) → DB 처리 → 커밋 후 RELEASED 전파
     */
    private void cancel(Booking booking, Long actorId, String cancelledBy, String reason, int refundAmount, String idemKey) {
        paymentRepository.findByBookingId(booking.getId())
                .map(Payment::getPaymentKey)
                .ifPresent(paymentKey -> requestTossCancel(paymentKey, booking.getBookingNumber(), reason, refundAmount, idemKey));

        List<String> seats;
        try {
            seats = bookingCancelTxService.cancel(booking.getId(), actorId, cancelledBy, reason, refundAmount);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("[예약 취소 DB 처리 실패: 수동 처리 필요 - 토스 취소는 완료] bookingNumber={}, refundAmount={}, by={}",
                    booking.getBookingNumber(), refundAmount, cancelledBy, e);
            throw business(HttpsStatusCode.INTERNAL_SERVER_ERROR);
        }

        publishReleased(booking, seats);
    }

    private void requestTossCancel(String paymentKey, String bookingNumber, String reason, int refundAmount, String idemKey) {
        try {
            tossPaymentService.cancel(paymentKey, reason, refundAmount, idemKey);
        } catch (TossApiException e) {
            if (ALREADY_CANCELED.equals(e.getCode())) {
                log.warn("[이미 취소된 결제] bookingNumber={}", bookingNumber);
                return;
            }
            throw business(e.getMessage(), HttpsStatusCode.UNPROCESSABLE_ENTITY);
        } catch (Exception e) {
            log.error("[토스 취소 응답 불명: 수동 확인 필요] bookingNumber={}, refundAmount={}", bookingNumber, refundAmount, e);
            throw business(HttpsStatusCode.GATEWAY_TIMEOUT);
        }
    }

    private void publishReleased(Booking booking, List<String> seats) {
        try {
            flightInventoryRepository.findByFlightIdAndFlightDate(booking.getFlightId(), booking.getFlightDate())
                    .ifPresent(inventory -> seatEventPublisher.publish(
                            SeatDto.EventType.RELEASED, inventory.getId(), seats, null));
        } catch (Exception e) {
            log.warn("[취소 좌석 전파 실패] bookingNumber={}, cause={}", booking.getBookingNumber(), e.getMessage());
        }
    }

}
