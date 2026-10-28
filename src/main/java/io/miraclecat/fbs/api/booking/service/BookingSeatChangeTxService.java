/**
 * packageName    : io.miraclecat.fbs.api.booking.service
 * fileName       : BookingSeatChangeTxService
 * author         : MiracleCat
 * date           : 2026-10-25
 * description    : 좌석 변경 DB 처리 (단일 트랜잭션)
 *                  - 예약 행 비관적 락 후 상태 재확인 → 탑승객 좌석 변경, 이력
 *                  - 대상 탑승객 좌석 확정 벌크 삭제 → 좌석 번호 순으로 INSERT (UNIQUE 최종 방어)
 */
package io.miraclecat.fbs.api.booking.service;

import io.miraclecat.fbs.api.booking.domain.entity.Booking;
import io.miraclecat.fbs.api.booking.domain.entity.BookingHistory;
import io.miraclecat.fbs.api.booking.domain.entity.BookingPassenger;
import io.miraclecat.fbs.api.booking.domain.repository.BookingHistoryRepository;
import io.miraclecat.fbs.api.booking.domain.repository.BookingPassengerRepository;
import io.miraclecat.fbs.api.booking.domain.repository.BookingRepository;
import io.miraclecat.fbs.api.flight.domain.entity.SeatReservation;
import io.miraclecat.fbs.api.flight.domain.repository.SeatReservationRepository;
import io.miraclecat.fbs.core.config.RequiredTx;
import io.miraclecat.fbs.core.response.HttpsStatusCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import static io.miraclecat.fbs.core.exception.GlobalException.business;

@Service
@RequiredArgsConstructor
public class BookingSeatChangeTxService {

    private static final String CHANGED_BY_USER = "USER";

    private final BookingRepository bookingRepository;
    private final BookingPassengerRepository bookingPassengerRepository;
    private final BookingHistoryRepository bookingHistoryRepository;
    private final SeatReservationRepository seatReservationRepository;

    /**
     * @param changes { 탑승객 ID: 새 좌석 번호 }
     */
    @RequiredTx
    public void changeSeats(Long bookingId, Long inventoryId, Map<Long, String> changes, Long memberId) {
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> business(HttpsStatusCode.RESOURCE_NOT_FOUND));
        if (!booking.isConfirmed()) {
            throw business("취소된 예약은 좌석을 변경할 수 없습니다.", HttpsStatusCode.UNPROCESSABLE_ENTITY);
        }

        StringJoiner history = new StringJoiner(", ", "좌석 변경: ", "");
        for (BookingPassenger passenger : bookingPassengerRepository.findByBookingIdOrderByIdAsc(bookingId)) {
            String newSeat = changes.get(passenger.getId());
            if (newSeat == null) continue;
            history.add(passenger.getSeatNumber() + " → " + newSeat);
            passenger.changeSeat(newSeat, memberId);
        }
        booking.touch(memberId);

        bookingHistoryRepository.save(BookingHistory.builder()
                .bookingId(bookingId)
                .previousStatus(booking.getStatus())
                .currentStatus(booking.getStatus())
                .changedBy(CHANGED_BY_USER)
                .reason(history.toString())
                .build());

        // 벌크 삭제(flush 후 clear)는 엔티티 변경 이후에 실행
        seatReservationRepository.deleteByBookingPassengerIds(changes.keySet());

        List<SeatReservation> seats = changes.entrySet().stream()
                .map(e -> SeatReservation.builder()
                        .inventoryId(inventoryId)
                        .seatNumber(e.getValue())
                        .bookingId(bookingId)
                        .bookingPassengerId(e.getKey())
                        .build())
                .sorted(Comparator.comparing(SeatReservation::getSeatNumber))
                .toList();
        seatReservationRepository.saveAll(seats);
        seatReservationRepository.flush();
    }

}
