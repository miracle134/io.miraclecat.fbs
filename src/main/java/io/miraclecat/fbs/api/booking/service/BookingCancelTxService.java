/**
 * packageName    : io.miraclecat.fbs.api.booking.service
 * fileName       : BookingCancelTxService
 * author         : MiracleCat
 * date           : 2026-10-25
 * description    : 예약 취소 DB 처리 (단일 트랜잭션)
 *                  - 예약 행 비관적 락 후 상태 재확인 → 좌석 확정 삭제, 재고 복원/로그, 결제 취소 기록, 이력
 */
package io.miraclecat.fbs.api.booking.service;

import io.miraclecat.fbs.api.booking.domain.entity.Booking;
import io.miraclecat.fbs.api.booking.domain.entity.BookingHistory;
import io.miraclecat.fbs.api.booking.domain.repository.BookingHistoryRepository;
import io.miraclecat.fbs.api.booking.domain.repository.BookingRepository;
import io.miraclecat.fbs.api.flight.domain.repository.SeatReservationRepository;
import io.miraclecat.fbs.api.flight.service.FlightInventoryService;
import io.miraclecat.fbs.api.payment.domain.repository.PaymentRepository;
import io.miraclecat.fbs.core.config.RequiredTx;
import io.miraclecat.fbs.core.response.HttpsStatusCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

import static io.miraclecat.fbs.core.exception.GlobalException.business;

@Service
@RequiredArgsConstructor
public class BookingCancelTxService {

    private final BookingRepository bookingRepository;
    private final BookingHistoryRepository bookingHistoryRepository;
    private final SeatReservationRepository seatReservationRepository;
    private final FlightInventoryService flightInventoryService;
    private final PaymentRepository paymentRepository;

    /**
     * @return 해제된 좌석 번호 목록 (커밋 후 RELEASED 전파용)
     */
    @RequiredTx
    public List<String> cancel(Long bookingId, Long memberId, String cancelledBy, String reason, Integer refundAmount) {
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> business(HttpsStatusCode.RESOURCE_NOT_FOUND));
        if (!booking.isConfirmed()) {
            throw business("이미 취소된 예약입니다.", HttpsStatusCode.UNPROCESSABLE_ENTITY);
        }

        List<String> seats = seatReservationRepository.findSeatNumbersByBookingId(bookingId);

        String previousStatus = booking.getStatus();
        booking.cancelBooking(cancelledBy, reason, memberId);

        paymentRepository.findByBookingId(bookingId)
                .ifPresent(payment -> payment.cancelPayment(refundAmount, reason));

        flightInventoryService.release(booking.getFlightId(), booking.getFlightDate(),
                booking.getPassengerCount(), bookingId);

        bookingHistoryRepository.save(BookingHistory.builder()
                .bookingId(bookingId)
                .previousStatus(previousStatus)
                .currentStatus(Booking.STATUS_CANCELLED)
                .changedBy(cancelledBy)
                .reason(reason)
                .build());

        // 벌크 삭제(flush 후 clear)는 엔티티 변경 이후 마지막에 실행
        seatReservationRepository.deleteByBookingId(bookingId);
        return seats;
    }

}
