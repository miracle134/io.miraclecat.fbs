package io.miraclecat.fbs.api.booking.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class BookingTest {

    private static final LocalDate DEPARTURE = LocalDate.of(2026, 11, 10);

    @Test
    @DisplayName("총 결제 금액 = 항공권 + 예약 수수료, 생성 즉시 confirmed")
    void totalAmount_isFarePlusFee() {
        Booking booking = booking(DEPARTURE);

        assertThat(booking.getTotalAmount()).isEqualTo(148_000);
        assertThat(booking.isConfirmed()).isTrue();
    }

    @Test
    @DisplayName("변경·취소는 출발 전날까지(날짜 기준), 출발 당일·이후는 불가")
    void changeableUntilDayBefore() {
        Booking booking = booking(DEPARTURE);

        assertThat(booking.isChangeableBy(DEPARTURE.minusDays(1))).isTrue();
        assertThat(booking.isChangeableBy(DEPARTURE)).isFalse();
        assertThat(booking.isChangeableBy(DEPARTURE.plusDays(1))).isFalse();
    }

    @Test
    @DisplayName("취소 시 상태·취소 주체·사유·일시 기록")
    void cancelBooking_recordsActor() {
        Booking booking = booking(DEPARTURE);

        booking.cancelBooking("ADMIN", "항공사 운항 취소", 1L);

        assertThat(booking.isConfirmed()).isFalse();
        assertThat(booking.getStatus()).isEqualTo(Booking.STATUS_CANCELLED);
        assertThat(booking.getCancelledBy()).isEqualTo("ADMIN");
        assertThat(booking.getCancelReason()).isEqualTo("항공사 운항 취소");
        assertThat(booking.getCancelledAt()).isNotNull();
    }

    private Booking booking(LocalDate flightDate) {
        return Booking.builder()
                .bookingNumber("BK20261101000001")
                .memberId(10L)
                .flightId(100L)
                .flightDate(flightDate)
                .passengerCount(2)
                .fareAmount(138_000)
                .bookingFeeAmount(10_000)
                .paymentMethod("CARD")
                .createdId(10L)
                .build();
    }
}
