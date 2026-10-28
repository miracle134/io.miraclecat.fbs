/**
 * packageName    : io.miraclecat.fbs.api.payment.service
 * fileName       : PaymentTxService
 * author         : MiracleCat
 * date           : 2026-10-25
 * description    : 결제 승인 후 예약 일괄 저장 (단일 트랜잭션)
 *                  - 토스 호출(PaymentCheckoutService)과 빈을 분리 → 외부 API는 트랜잭션 밖, 프록시를 거쳐 트랜잭션 적용
 *                  - 저장 순서: 예약 → 탑승객 → 좌석 확정(UNIQUE) → 재고 차감(원자적 UPDATE)/로그 → 결제 → 이력
 *                  - 좌석 중복은 UNIQUE 위반, 좌석 부족은 UPDATE 0건으로 이 메서드 안에서 예외가 됨 → 전체 롤백
 */
package io.miraclecat.fbs.api.payment.service;

import io.miraclecat.fbs.api.booking.domain.entity.Booking;
import io.miraclecat.fbs.api.booking.domain.entity.BookingHistory;
import io.miraclecat.fbs.api.booking.domain.entity.BookingPassenger;
import io.miraclecat.fbs.api.booking.domain.repository.BookingHistoryRepository;
import io.miraclecat.fbs.api.booking.domain.repository.BookingPassengerRepository;
import io.miraclecat.fbs.api.booking.domain.repository.BookingRepository;
import io.miraclecat.fbs.api.booking.dto.BookingSessionDto;
import io.miraclecat.fbs.api.flight.domain.entity.SeatReservation;
import io.miraclecat.fbs.api.flight.domain.repository.SeatReservationRepository;
import io.miraclecat.fbs.api.flight.service.FlightInventoryService;
import io.miraclecat.fbs.api.payment.domain.entity.Payment;
import io.miraclecat.fbs.api.payment.domain.repository.PaymentRepository;
import io.miraclecat.fbs.core.config.RequiredTx;
import io.miraclecat.fbs.pg.toss.TossPaymentDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentTxService {

    private static final String CHANGED_BY_USER = "USER";

    private final BookingRepository bookingRepository;
    private final BookingPassengerRepository bookingPassengerRepository;
    private final BookingHistoryRepository bookingHistoryRepository;
    private final SeatReservationRepository seatReservationRepository;
    private final FlightInventoryService flightInventoryService;
    private final PaymentRepository paymentRepository;

    @RequiredTx
    public Booking saveBooking(BookingSessionDto.Session session, TossPaymentDto.ConfirmRes toss, Long memberId) {
        String paymentMethod = toPaymentMethod(toss.getMethod());

        Booking booking = Booking.builder()
                .bookingNumber(session.getOrderId())
                .memberId(memberId)
                .flightId(session.getFlightId())
                .flightDate(LocalDate.parse(session.getFlightDate()))
                .passengerCount(session.getPassengerCount())
                .fareAmount(session.getFareAmount())
                .bookingFeeAmount(session.getBookingFeeAmount())
                .paymentMethod(paymentMethod)
                .createdId(memberId)
                .build();
        bookingRepository.save(booking);

        List<BookingPassenger> passengers = session.getPassengers().stream()
                .map(p -> BookingPassenger.builder()
                        .bookingId(booking.getId())
                        .name(p.getName())
                        .nameEn(p.getNameEn())
                        .email(p.getEmail())
                        .mobile(p.getMobile())
                        .birthDate(LocalDate.parse(p.getBirthDate()))
                        .gender(p.getGender())
                        .seatNumber(p.getSeatNumber())
                        .createdId(memberId)
                        .build())
                .toList();
        bookingPassengerRepository.saveAll(passengers);

        List<SeatReservation> seats = new ArrayList<>();
        for (BookingPassenger passenger : passengers) {
            seats.add(SeatReservation.builder()
                    .inventoryId(session.getInventoryId())
                    .seatNumber(passenger.getSeatNumber())
                    .bookingId(booking.getId())
                    .bookingPassengerId(passenger.getId())
                    .build());
        }
        seats.sort(Comparator.comparing(SeatReservation::getSeatNumber));
        seatReservationRepository.saveAll(seats);

        flightInventoryService.reserve(session.getInventoryId(), session.getPassengerCount(), booking.getId());

        paymentRepository.save(Payment.builder()
                .bookingId(booking.getId())
                .paymentKey(toss.getPaymentKey())
                .amount(session.getTotalAmount())
                .method(paymentMethod)
                .approvedAt(parseApprovedAt(toss.getApprovedAt()))
                .build());

        bookingHistoryRepository.save(BookingHistory.builder()
                .bookingId(booking.getId())
                .previousStatus(null)
                .currentStatus(Booking.STATUS_CONFIRMED)
                .changedBy(CHANGED_BY_USER)
                .reason("결제 완료")
                .build());

        bookingRepository.flush();
        return booking;
    }

    /**
     * 토스 결제수단(한글) → 저장 코드
     */
    private String toPaymentMethod(String tossMethod) {
        if (tossMethod == null) return "CARD";
        return switch (tossMethod) {
            case "카드" -> "CARD";
            case "계좌이체" -> "BANK_TRANSFER";
            case "가상계좌" -> "VIRTUAL_ACCOUNT";
            case "간편결제" -> "EASY_PAY";
            case "휴대폰" -> "MOBILE_PHONE";
            default -> tossMethod.length() > 20 ? tossMethod.substring(0, 20) : tossMethod;
        };
    }

    private LocalDateTime parseApprovedAt(String approvedAt) {
        if (approvedAt == null) return null;
        try {
            return OffsetDateTime.parse(approvedAt).toLocalDateTime();
        } catch (DateTimeParseException e) {
            return null;
        }
    }

}
