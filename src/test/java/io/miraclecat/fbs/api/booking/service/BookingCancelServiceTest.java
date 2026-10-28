package io.miraclecat.fbs.api.booking.service;

import io.miraclecat.fbs.api.booking.domain.entity.Booking;
import io.miraclecat.fbs.api.booking.domain.repository.BookingRepository;
import io.miraclecat.fbs.api.booking.dto.BookingDto;
import io.miraclecat.fbs.api.flight.domain.repository.FlightInventoryRepository;
import io.miraclecat.fbs.api.flight.websocket.SeatEventPublisher;
import io.miraclecat.fbs.api.payment.domain.entity.Payment;
import io.miraclecat.fbs.api.payment.domain.repository.PaymentRepository;
import io.miraclecat.fbs.core.auth.AuthRole;
import io.miraclecat.fbs.core.exception.BusinessException;
import io.miraclecat.fbs.core.response.HttpsStatusCode;
import io.miraclecat.fbs.pg.toss.TossApiException;
import io.miraclecat.fbs.pg.toss.TossPaymentService;
import io.miraclecat.fbs.support.TestSecurity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 예약 취소 정책 — 취소 기한, 환불 금액(항공권 / 전액), 멱등키, 토스 실패 처리
 * 금액: 항공권 138,000원(2인) + 예약 수수료 10,000원 = 148,000원
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BookingCancelServiceTest {

    private static final Long BOOKING_ID = 1L;
    private static final Long OWNER_ID = 10L;
    private static final Long ADMIN_ID = 1L;
    private static final String BOOKING_NUMBER = "BK20261101000001";
    private static final String PAYMENT_KEY = "pay-key-1";
    private static final int FARE = 138_000;
    private static final int FEE = 10_000;

    @Mock BookingRepository bookingRepository;
    @Mock PaymentRepository paymentRepository;
    @Mock FlightInventoryRepository flightInventoryRepository;
    @Mock TossPaymentService tossPaymentService;
    @Mock BookingCancelTxService bookingCancelTxService;
    @Mock SeatEventPublisher seatEventPublisher;

    @InjectMocks BookingCancelService bookingCancelService;

    @AfterEach
    void tearDown() {
        TestSecurity.clear();
    }

    @Nested
    @DisplayName("사용자 취소")
    class ByUser {

        @Test
        @DisplayName("출발 전날까지 취소 가능, 항공권 금액만 환불 (예약 수수료 제외)")
        void refundsFareOnly() {
            TestSecurity.loginAs(OWNER_ID, AuthRole.ROLE_USER);
            givenBooking(LocalDate.now().plusDays(1));

            bookingCancelService.cancelByUser(BOOKING_ID, null);

            verify(tossPaymentService).cancel(eq(PAYMENT_KEY), anyString(), eq(FARE), eq(BOOKING_NUMBER + "-cancel"));
            verify(bookingCancelTxService).cancel(eq(BOOKING_ID), eq(OWNER_ID), eq("USER"), anyString(), eq(FARE));
        }

        @Test
        @DisplayName("출발 당일에는 취소 불가 — 토스 호출 없음")
        void departureDay_rejected() {
            TestSecurity.loginAs(OWNER_ID, AuthRole.ROLE_USER);
            givenBooking(LocalDate.now());

            assertThatThrownBy(() -> bookingCancelService.cancelByUser(BOOKING_ID, null))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getStatusCode())
                    .isEqualTo(HttpsStatusCode.UNPROCESSABLE_ENTITY);

            verifyNoInteractions(tossPaymentService, bookingCancelTxService);
        }

        @Test
        @DisplayName("다른 회원의 예약은 취소 불가 (ACCESS_DENIED)")
        void otherMember_denied() {
            TestSecurity.loginAs(99L, AuthRole.ROLE_USER);
            givenBooking(LocalDate.now().plusDays(3));

            assertThatThrownBy(() -> bookingCancelService.cancelByUser(BOOKING_ID, null))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getStatusCode())
                    .isEqualTo(HttpsStatusCode.ACCESS_DENIED);

            verifyNoInteractions(tossPaymentService);
        }

        @Test
        @DisplayName("비로그인은 LOGIN_REQUIRED")
        void anonymous_loginRequired() {
            assertThatThrownBy(() -> bookingCancelService.cancelByUser(BOOKING_ID, null))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getStatusCode())
                    .isEqualTo(HttpsStatusCode.LOGIN_REQUIRED);
        }

        @Test
        @DisplayName("토스에서 이미 취소된 결제(ALREADY_CANCELED_PAYMENT)면 DB 취소는 계속 진행")
        void alreadyCanceledAtToss_continuesDbCancel() {
            TestSecurity.loginAs(OWNER_ID, AuthRole.ROLE_USER);
            givenBooking(LocalDate.now().plusDays(2));
            when(tossPaymentService.cancel(any(), any(), any(), any()))
                    .thenThrow(new TossApiException(400, "ALREADY_CANCELED_PAYMENT", "이미 취소된 결제입니다."));

            bookingCancelService.cancelByUser(BOOKING_ID, null);

            verify(bookingCancelTxService).cancel(eq(BOOKING_ID), eq(OWNER_ID), eq("USER"), anyString(), eq(FARE));
        }

        @Test
        @DisplayName("토스 취소가 다른 이유로 실패하면 DB는 건드리지 않음")
        void tossFailure_keepsDbUntouched() {
            TestSecurity.loginAs(OWNER_ID, AuthRole.ROLE_USER);
            givenBooking(LocalDate.now().plusDays(2));
            when(tossPaymentService.cancel(any(), any(), any(), any()))
                    .thenThrow(new TossApiException(400, "NOT_CANCELABLE_AMOUNT", "취소할 수 없는 금액입니다."));

            assertThatThrownBy(() -> bookingCancelService.cancelByUser(BOOKING_ID, null))
                    .isInstanceOf(BusinessException.class);

            verifyNoInteractions(bookingCancelTxService);
        }
    }

    @Nested
    @DisplayName("관리자 취소")
    class ByAdmin {

        @Test
        @DisplayName("FULL: 결제 금액 전액(수수료 포함) 환불, 출발일 제한 없음")
        void full_refundsTotal() {
            TestSecurity.loginAs(ADMIN_ID, AuthRole.ROLE_ADMIN);
            givenBooking(LocalDate.now());

            bookingCancelService.cancelByAdmin(BOOKING_ID, adminReq("FULL"));

            verify(tossPaymentService).cancel(eq(PAYMENT_KEY), anyString(), eq(FARE + FEE), eq(BOOKING_NUMBER + "-cancel-full"));
            verify(bookingCancelTxService).cancel(eq(BOOKING_ID), eq(ADMIN_ID), eq("ADMIN"), anyString(), eq(FARE + FEE));
        }

        @Test
        @DisplayName("FARE: 항공권 금액만 환불")
        void fare_refundsFareOnly() {
            TestSecurity.loginAs(ADMIN_ID, AuthRole.ROLE_ADMIN);
            givenBooking(LocalDate.now().plusDays(5));

            bookingCancelService.cancelByAdmin(BOOKING_ID, adminReq("FARE"));

            verify(tossPaymentService).cancel(eq(PAYMENT_KEY), anyString(), eq(FARE), eq(BOOKING_NUMBER + "-cancel-fare"));
        }

        @Test
        @DisplayName("환불 방식이 없거나 잘못되면 INVALID_PARAMETER")
        void invalidRefundType_rejected() {
            TestSecurity.loginAs(ADMIN_ID, AuthRole.ROLE_ADMIN);
            givenBooking(LocalDate.now().plusDays(5));

            assertThatThrownBy(() -> bookingCancelService.cancelByAdmin(BOOKING_ID, adminReq("HALF")))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getStatusCode())
                    .isEqualTo(HttpsStatusCode.INVALID_PARAMETER);

            verifyNoInteractions(tossPaymentService);
        }
    }

    private void givenBooking(LocalDate flightDate) {
        Booking booking = Booking.builder()
                .bookingNumber(BOOKING_NUMBER)
                .memberId(OWNER_ID)
                .flightId(100L)
                .flightDate(flightDate)
                .passengerCount(2)
                .fareAmount(FARE)
                .bookingFeeAmount(FEE)
                .paymentMethod("CARD")
                .createdId(OWNER_ID)
                .build();
        ReflectionTestUtils.setField(booking, "id", BOOKING_ID);
        Payment payment = Payment.builder()
                .bookingId(BOOKING_ID)
                .paymentKey(PAYMENT_KEY)
                .amount(FARE + FEE)
                .method("CARD")
                .build();

        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));
        when(paymentRepository.findByBookingId(any())).thenReturn(Optional.of(payment));
        when(bookingCancelTxService.cancel(any(), any(), any(), any(), any())).thenReturn(List.of("12A", "12B"));
        when(flightInventoryRepository.findByFlightIdAndFlightDate(any(), any())).thenReturn(Optional.empty());
    }

    private BookingDto.AdminCancelReq adminReq(String refundType) {
        BookingDto.AdminCancelReq req = new BookingDto.AdminCancelReq();
        req.setRefundType(refundType);
        return req;
    }
}
