package io.miraclecat.fbs.api.payment.service;

import io.miraclecat.fbs.api.booking.domain.entity.Booking;
import io.miraclecat.fbs.api.booking.domain.repository.BookingRepository;
import io.miraclecat.fbs.api.booking.dto.BookingSessionDto;
import io.miraclecat.fbs.api.booking.service.BookingMailService;
import io.miraclecat.fbs.api.booking.service.BookingNumberGenerator;
import io.miraclecat.fbs.api.booking.service.BookingSessionService;
import io.miraclecat.fbs.api.flight.domain.repository.FlightRepository;
import io.miraclecat.fbs.api.flight.dto.SeatDto;
import io.miraclecat.fbs.api.flight.redis.SeatHoldStore;
import io.miraclecat.fbs.api.flight.websocket.SeatEventPublisher;
import io.miraclecat.fbs.api.payment.dto.PaymentDto;
import io.miraclecat.fbs.core.auth.AuthRole;
import io.miraclecat.fbs.core.exception.BusinessException;
import io.miraclecat.fbs.core.response.HttpsStatusCode;
import io.miraclecat.fbs.pg.toss.TossPaymentDto;
import io.miraclecat.fbs.pg.toss.TossPaymentService;
import io.miraclecat.fbs.support.TestSecurity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static io.miraclecat.fbs.core.exception.GlobalException.business;

/**
 * 결제 승인 흐름 — 토스 승인 후 DB 저장 실패 시 보상(자동 취소) 동작 검증
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PaymentCheckoutServiceTest {

    private static final Long MEMBER_ID = 10L;
    private static final Long INVENTORY_ID = 1L;
    private static final String SESSION_ID = "session-1";
    private static final String ORDER_ID = "BK20261101000001";
    private static final String PAYMENT_KEY = "pay-key-1";
    private static final int TOTAL_AMOUNT = 74_000;

    @Mock BookingSessionService bookingSessionService;
    @Mock BookingNumberGenerator bookingNumberGenerator;
    @Mock BookingRepository bookingRepository;
    @Mock FlightRepository flightRepository;
    @Mock SeatHoldStore seatHoldStore;
    @Mock SeatEventPublisher seatEventPublisher;
    @Mock TossPaymentService tossPaymentService;
    @Mock PaymentTxService paymentTxService;
    @Mock BookingMailService bookingMailService;

    @InjectMocks PaymentCheckoutService paymentCheckoutService;

    private BookingSessionDto.Session session;

    @Test
    @DisplayName("좌석 부족(원자적 UPDATE 0건) → 토스 결제 자동 취소 후 DUPLICATE_ITEM")
    void seatShortage_cancelsPayment() {
        when(paymentTxService.saveBooking(any(), any(), eq(MEMBER_ID)))
                .thenThrow(business("남은 좌석이 부족합니다.", HttpsStatusCode.UNPROCESSABLE_ENTITY));

        assertThatThrownBy(() -> paymentCheckoutService.confirm(confirmReq(TOTAL_AMOUNT)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getStatusCode())
                .isEqualTo(HttpsStatusCode.DUPLICATE_ITEM);

        verify(tossPaymentService).cancel(eq(PAYMENT_KEY), anyString(), isNull(), eq(ORDER_ID + "-auto-cancel"));
        verify(bookingMailService, never()).sendBookingConfirmed(any(), any());
    }

    @BeforeEach
    void setUp() {
        TestSecurity.loginAs(MEMBER_ID, AuthRole.ROLE_USER);

        session = BookingSessionDto.Session.builder()
                .sessionId(SESSION_ID)
                .memberId(MEMBER_ID)
                .flightId(100L)
                .inventoryId(INVENTORY_ID)
                .flightDate(LocalDate.now().plusDays(7).toString())
                .passengerCount(1)
                .fareAmount(69_000)
                .bookingFeeAmount(5_000)
                .totalAmount(TOTAL_AMOUNT)
                .orderId(ORDER_ID)
                .passengers(List.of(BookingSessionDto.Passenger.builder()
                        .name("홍길동").nameEn("HONG GILDONG").email("hong@test.com")
                        .mobile("010-1234-5678").birthDate("1990-01-01").gender("male")
                        .seatNumber("12A").build()))
                .build();

        when(bookingRepository.findByBookingNumber(ORDER_ID)).thenReturn(Optional.empty());
        when(bookingSessionService.findOwnedSession(SESSION_ID, MEMBER_ID)).thenReturn(Optional.of(session));
        when(seatHoldStore.findActiveHolds(INVENTORY_ID)).thenReturn(Map.of("12A", String.valueOf(MEMBER_ID)));
        when(tossPaymentService.confirm(PAYMENT_KEY, ORDER_ID, TOTAL_AMOUNT)).thenReturn(tossDone());
    }

    @AfterEach
    void tearDown() {
        TestSecurity.clear();
    }

    @Test
    @DisplayName("좌석 UNIQUE 위반 → 토스 결제 자동 취소 후 DUPLICATE_ITEM")
    void seatUniqueViolation_cancelsPayment() {
        when(paymentTxService.saveBooking(any(), any(), eq(MEMBER_ID)))
                .thenThrow(new DataIntegrityViolationException("uk_inventory_seat"));

        assertThatThrownBy(() -> paymentCheckoutService.confirm(confirmReq(TOTAL_AMOUNT)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getStatusCode())
                .isEqualTo(HttpsStatusCode.DUPLICATE_ITEM);

        verify(tossPaymentService).cancel(eq(PAYMENT_KEY), anyString(), isNull(), eq(ORDER_ID + "-auto-cancel"));
        verify(bookingMailService, never()).sendBookingConfirmed(any(), any());
        verify(seatEventPublisher, never()).publish(any(), any(), any(), any());
    }

    @Test
    @DisplayName("재고 낙관적 락 충돌 → 토스 결제 자동 취소")
    void optimisticLockConflict_cancelsPayment() {
        when(paymentTxService.saveBooking(any(), any(), eq(MEMBER_ID)))
                .thenThrow(new OptimisticLockingFailureException("version mismatch"));

        assertThatThrownBy(() -> paymentCheckoutService.confirm(confirmReq(TOTAL_AMOUNT)))
                .isInstanceOf(BusinessException.class);

        verify(tossPaymentService).cancel(eq(PAYMENT_KEY), anyString(), isNull(), eq(ORDER_ID + "-auto-cancel"));
    }

    @Test
    @DisplayName("예상 못한 기술 실패 → 자동 취소하지 않고 수동 처리 대상(PAYMENT_BOOKING_FAILED)")
    void technicalFailure_doesNotCancel() {
        when(paymentTxService.saveBooking(any(), any(), eq(MEMBER_ID)))
                .thenThrow(new IllegalStateException("db connection lost"));

        assertThatThrownBy(() -> paymentCheckoutService.confirm(confirmReq(TOTAL_AMOUNT)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getStatusCode())
                .isEqualTo(HttpsStatusCode.PAYMENT_BOOKING_FAILED);

        verify(tossPaymentService, never()).cancel(any(), any(), any(), any());
    }

    @Test
    @DisplayName("클라이언트 금액이 세션 금액과 다르면 토스 승인을 호출하지 않음 (위변조 차단)")
    void amountMismatch_rejectsBeforeToss() {
        assertThatThrownBy(() -> paymentCheckoutService.confirm(confirmReq(1_000)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getStatusCode())
                .isEqualTo(HttpsStatusCode.INVALID_PARAMETER);

        verify(tossPaymentService, never()).confirm(any(), any(), any());
        verify(paymentTxService, never()).saveBooking(any(), any(), any());
    }

    @Test
    @DisplayName("선점이 만료된 좌석이면 토스 승인을 호출하지 않음")
    void holdExpired_rejectsBeforeToss() {
        when(seatHoldStore.findActiveHolds(INVENTORY_ID)).thenReturn(Map.of("12A", "999"));

        assertThatThrownBy(() -> paymentCheckoutService.confirm(confirmReq(TOTAL_AMOUNT)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getStatusCode())
                .isEqualTo(HttpsStatusCode.DUPLICATE_ITEM);

        verify(tossPaymentService, never()).confirm(any(), any(), any());
    }

    @Test
    @DisplayName("토스 승인 상태가 DONE이 아니면 저장하지 않고 자동 취소")
    void tossNotDone_cancelsWithoutSaving() {
        TossPaymentDto.ConfirmRes inProgress = tossDone();
        inProgress.setStatus("IN_PROGRESS");
        when(tossPaymentService.confirm(PAYMENT_KEY, ORDER_ID, TOTAL_AMOUNT)).thenReturn(inProgress);

        assertThatThrownBy(() -> paymentCheckoutService.confirm(confirmReq(TOTAL_AMOUNT)))
                .isInstanceOf(BusinessException.class);

        verify(paymentTxService, never()).saveBooking(any(), any(), any());
        verify(tossPaymentService).cancel(eq(PAYMENT_KEY), anyString(), isNull(), eq(ORDER_ID + "-auto-cancel"));
    }

    @Test
    @DisplayName("이미 저장된 예약번호로 재호출(새로고침) → 토스 재승인 없이 성공")
    void duplicateConfirm_isIdempotent() {
        Booking saved = Booking.builder()
                .bookingNumber(ORDER_ID).memberId(MEMBER_ID).flightId(100L)
                .flightDate(LocalDate.now().plusDays(7)).passengerCount(1)
                .fareAmount(69_000).bookingFeeAmount(5_000).paymentMethod("CARD").createdId(MEMBER_ID)
                .build();
        when(bookingRepository.findByBookingNumber(ORDER_ID)).thenReturn(Optional.of(saved));

        var res = paymentCheckoutService.confirm(confirmReq(TOTAL_AMOUNT));

        assertThat(res.getBody().getResult().getBookingNumber()).isEqualTo(ORDER_ID);
        verify(tossPaymentService, never()).confirm(any(), any(), any());
        verify(paymentTxService, never()).saveBooking(any(), any(), any());
    }

    @Test
    @DisplayName("정상 승인 → 저장 후 메일 발송·BOOKED 전파, 결제 취소 없음")
    void success_savesAndPublishes() {
        Booking booking = Booking.builder()
                .bookingNumber(ORDER_ID).memberId(MEMBER_ID).flightId(100L)
                .flightDate(LocalDate.now().plusDays(7)).passengerCount(1)
                .fareAmount(69_000).bookingFeeAmount(5_000).paymentMethod("CARD").createdId(MEMBER_ID)
                .build();
        when(paymentTxService.saveBooking(any(), any(), eq(MEMBER_ID))).thenReturn(booking);

        var res = paymentCheckoutService.confirm(confirmReq(TOTAL_AMOUNT));

        assertThat(res.getBody().getResult().getBookingNumber()).isEqualTo(ORDER_ID);
        verify(bookingMailService).sendBookingConfirmed(booking, session);
        verify(seatEventPublisher).publish(eq(SeatDto.EventType.BOOKED), eq(INVENTORY_ID), eq(List.of("12A")), any());
        verify(tossPaymentService, never()).cancel(any(), any(), any(), any());
    }

    private PaymentDto.ConfirmReq confirmReq(int amount) {
        PaymentDto.ConfirmReq req = new PaymentDto.ConfirmReq();
        req.setSessionId(SESSION_ID);
        req.setPaymentKey(PAYMENT_KEY);
        req.setOrderId(ORDER_ID);
        req.setAmount(amount);
        req.setClientId("client-1");
        return req;
    }

    private TossPaymentDto.ConfirmRes tossDone() {
        return TossPaymentDto.ConfirmRes.builder()
                .paymentKey(PAYMENT_KEY)
                .orderId(ORDER_ID)
                .status("DONE")
                .method("카드")
                .totalAmount(TOTAL_AMOUNT)
                .approvedAt("2026-10-27T10:00:00+09:00")
                .build();
    }
}
