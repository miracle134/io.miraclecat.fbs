/**
 * packageName    : io.miraclecat.fbs.api.payment.service
 * fileName       : PaymentCheckoutService
 * author         : MiracleCat
 * date           : 2026-10-25
 * description    : 결제 흐름 (트랜잭션 없음 — 토스 호출과 DB 저장을 분리)
 *
 * prepare (결제하기)
 *   세션·탑승객·선점 검증 → 세션 연장(10분 보장/25분 상한/3분 미만 차단) → 선점을 세션 만료까지 연장 → 주문번호 발급
 *
 * confirm (토스 successUrl 복귀)
 *   세션·주문번호·금액·선점 검증 → 토스 승인 → PaymentTxService 일괄 저장
 *   - 비즈니스 실패(좌석 UNIQUE 위반, 재고 충돌·부족): 토스 즉시 취소
 *   - 기술 실패 / 승인 응답 불명: ERROR 로그 후 수동 처리
 *   → 커밋 이후 선점·세션 정리, BOOKED 전파
 */
package io.miraclecat.fbs.api.payment.service;

import io.miraclecat.fbs.api.booking.domain.entity.Booking;
import io.miraclecat.fbs.api.booking.domain.repository.BookingRepository;
import io.miraclecat.fbs.api.booking.dto.BookingSessionDto;
import io.miraclecat.fbs.api.booking.service.BookingMailService;
import io.miraclecat.fbs.api.booking.service.BookingNumberGenerator;
import io.miraclecat.fbs.api.booking.service.BookingSessionService;
import io.miraclecat.fbs.api.flight.domain.entity.Flight;
import io.miraclecat.fbs.api.flight.domain.repository.FlightRepository;
import io.miraclecat.fbs.api.flight.dto.SeatDto;
import io.miraclecat.fbs.api.flight.redis.SeatHoldStore;
import io.miraclecat.fbs.api.flight.websocket.SeatEventPublisher;
import io.miraclecat.fbs.api.payment.dto.PaymentDto;
import io.miraclecat.fbs.core.exception.BusinessException;
import io.miraclecat.fbs.core.response.ApiEntity;
import io.miraclecat.fbs.core.response.HttpsStatusCode;
import io.miraclecat.fbs.core.utils.security.SecurityUtil;
import io.miraclecat.fbs.pg.toss.TossApiException;
import io.miraclecat.fbs.pg.toss.TossPaymentDto;
import io.miraclecat.fbs.pg.toss.TossPaymentService;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import static io.miraclecat.fbs.core.exception.GlobalException.business;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentCheckoutService {

    private static final Pattern SEAT_PATTERN = Pattern.compile("^[1-9]\\d*[A-F]$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$");
    private static final Pattern MOBILE_PATTERN = Pattern.compile("^[0-9+\\-()]{9,20}$");
    private static final Set<String> GENDERS = Set.of("male", "female");
    private static final String TOSS_DONE = "DONE";

    private final BookingSessionService bookingSessionService;
    private final BookingNumberGenerator bookingNumberGenerator;
    private final BookingRepository bookingRepository;
    private final FlightRepository flightRepository;
    private final SeatHoldStore seatHoldStore;
    private final SeatEventPublisher seatEventPublisher;
    private final TossPaymentService tossPaymentService;
    private final PaymentTxService paymentTxService;
    private final BookingMailService bookingMailService;

    /**
     * 결제 준비 (결제하기 클릭)
     */
    public ApiEntity<PaymentDto.PrepareRes> prepare(PaymentDto.PrepareReq req) {
        Long memberId = currentMemberId();
        BookingSessionDto.Session session = findSession(req.getSessionId(), memberId);

        List<BookingSessionDto.Passenger> passengers = validatePassengers(req.getPassengers(), session.getPassengerCount());
        validateMyHolds(session.getInventoryId(), memberId,
                passengers.stream().map(BookingSessionDto.Passenger::getSeatNumber).toList());

        session = bookingSessionService.extendForPayment(session);
        seatHoldStore.extendAll(session.getInventoryId(), memberId, session.getExpireAt() - System.currentTimeMillis());

        session.setOrderId(bookingNumberGenerator.generate());
        session.setPassengers(passengers);
        bookingSessionService.save(session);

        Flight flight = flightRepository.findById(session.getFlightId())
                .orElseThrow(() -> business(HttpsStatusCode.RESOURCE_NOT_FOUND));
        String orderName = flight.getFlightNumber() + " " + session.getDepartureAirport() + "→"
                + session.getArrivalAirport() + " (" + session.getPassengerCount() + "명)";

        PaymentDto.PrepareRes res = PaymentDto.PrepareRes.builder()
                .orderId(session.getOrderId())
                .orderName(orderName)
                .amount(session.getTotalAmount())
                .customerName(passengers.get(0).getName())
                .customerEmail(passengers.get(0).getEmail())
                .expireAt(session.getExpireAt())
                .serverTime(System.currentTimeMillis())
                .build();
        return ApiEntity.of(HttpsStatusCode.SUCCESS, res);
    }

    /**
     * 결제 승인 (토스 successUrl 복귀 후 호출)
     */
    public ApiEntity<PaymentDto.ConfirmRes> confirm(PaymentDto.ConfirmReq req) {
        Long memberId = currentMemberId();

        // 새로고침 등 중복 호출: 이미 저장된 예약이면 그대로 성공
        Optional<Booking> saved = bookingRepository.findByBookingNumber(req.getOrderId());
        if (saved.isPresent() && saved.get().isOwnedBy(memberId)) {
            return ApiEntity.of(HttpsStatusCode.SUCCESS, toConfirmRes(saved.get()));
        }

        BookingSessionDto.Session session = bookingSessionService.findOwnedSession(req.getSessionId(), memberId)
                .orElseThrow(() -> business("예약 가능 시간이 만료되어 결제가 진행되지 않았습니다.", HttpsStatusCode.REQUEST_TIMEOUT));
        if (session.getOrderId() == null || !session.getOrderId().equals(req.getOrderId()) || session.getPassengers() == null) {
            throw business("주문 정보가 일치하지 않습니다.", HttpsStatusCode.INVALID_PARAMETER);
        }
        if (!session.getTotalAmount().equals(req.getAmount())) {
            throw business("결제 금액이 일치하지 않습니다.", HttpsStatusCode.INVALID_PARAMETER);
        }

        List<String> seats = session.getPassengers().stream().map(BookingSessionDto.Passenger::getSeatNumber).toList();
        validateMyHolds(session.getInventoryId(), memberId, seats);

        TossPaymentDto.ConfirmRes toss = requestTossConfirm(req.getPaymentKey(), session);

        Booking booking;
        try {
            booking = paymentTxService.saveBooking(session, toss, memberId);
        } catch (DataIntegrityViolationException | OptimisticLockingFailureException
                 | OptimisticLockException | BusinessException e) {
            log.warn("[결제-예약 저장 실패: 비즈니스] orderId={}, paymentKey={}, cause={}",
                    session.getOrderId(), toss.getPaymentKey(), e.getMessage());
            cancelQuietly(toss.getPaymentKey(), session.getOrderId(), "좌석 확보 실패로 자동 취소");
            throw business("선택한 좌석이 이미 예약되어 결제가 자동 취소되었습니다. 좌석을 다시 선택해주세요.", HttpsStatusCode.DUPLICATE_ITEM);
        } catch (Exception e) {
            log.error("[결제-예약 저장 실패: 수동 처리 필요] orderId={}, paymentKey={}, amount={}",
                    session.getOrderId(), toss.getPaymentKey(), session.getTotalAmount(), e);
            throw business(HttpsStatusCode.PAYMENT_BOOKING_FAILED);
        }

        afterBooked(session, memberId, seats, req.getClientId());
        bookingMailService.sendBookingConfirmed(booking, session);
        return ApiEntity.of(HttpsStatusCode.REGISTER, toConfirmRes(booking));
    }

    /**
     * 토스 승인 (트랜잭션 밖)
     */
    private TossPaymentDto.ConfirmRes requestTossConfirm(String paymentKey, BookingSessionDto.Session session) {
        TossPaymentDto.ConfirmRes toss;
        try {
            toss = tossPaymentService.confirm(paymentKey, session.getOrderId(), session.getTotalAmount());
        } catch (TossApiException e) {
            throw business(e.getMessage(), HttpsStatusCode.UNPROCESSABLE_ENTITY);
        } catch (Exception e) {
            log.error("[결제 승인 응답 불명: 수동 확인 필요] orderId={}, paymentKey={}, amount={}",
                    session.getOrderId(), paymentKey, session.getTotalAmount(), e);
            throw business(HttpsStatusCode.PAYMENT_CONFIRM_UNKNOWN);
        }

        if (toss == null || !TOSS_DONE.equals(toss.getStatus())) {
            log.warn("[결제 승인 상태 비정상] orderId={}, status={}", session.getOrderId(), toss != null ? toss.getStatus() : null);
            cancelQuietly(paymentKey, session.getOrderId(), "결제 상태 비정상으로 자동 취소");
            throw business("결제가 완료되지 않았습니다. 다시 시도해주세요.", HttpsStatusCode.UNPROCESSABLE_ENTITY);
        }
        return toss;
    }

    /**
     * 커밋 이후 정리: 선점 해제(전파 없이), 세션 삭제, BOOKED 전파
     */
    private void afterBooked(BookingSessionDto.Session session, Long memberId, List<String> seats, String clientId) {
        try {
            seats.forEach(seat -> seatHoldStore.release(session.getInventoryId(), seat, memberId));
            bookingSessionService.delete(session.getSessionId());
            seatEventPublisher.publish(SeatDto.EventType.BOOKED, session.getInventoryId(), seats, clientId);
        } catch (Exception e) {
            log.warn("[예약 완료 후 정리 실패] orderId={}, cause={}", session.getOrderId(), e.getMessage());
        }
    }

    /**
     * 토스 전액 취소 (실패해도 예외를 던지지 않고 로그로 남김)
     */
    private void cancelQuietly(String paymentKey, String orderId, String reason) {
        try {
            tossPaymentService.cancel(paymentKey, reason, null, orderId + "-auto-cancel");
        } catch (Exception e) {
            log.error("[자동 취소 실패: 수동 처리 필요] orderId={}, paymentKey={}", orderId, paymentKey, e);
        }
    }

    /**
     * 내 선점 좌석인지 확인
     */
    private void validateMyHolds(Long inventoryId, Long memberId, List<String> seats) {
        Map<String, String> holds = seatHoldStore.findActiveHolds(inventoryId);
        String me = String.valueOf(memberId);
        List<String> lost = seats.stream().filter(seat -> !me.equals(holds.get(seat))).toList();
        if (!lost.isEmpty()) {
            throw business("선택한 좌석(" + String.join(", ", lost) + ")의 선점이 만료되었습니다. 좌석을 다시 선택해주세요.",
                    HttpsStatusCode.DUPLICATE_ITEM);
        }
    }

    /**
     * 탑승객 정보 검증 → 세션 저장 형식으로 변환
     */
    private List<BookingSessionDto.Passenger> validatePassengers(List<PaymentDto.PassengerReq> reqs, int passengerCount) {
        if (reqs == null || reqs.size() != passengerCount) {
            throw business("탑승객 수가 일치하지 않습니다.", HttpsStatusCode.INVALID_PARAMETER);
        }

        Set<String> seats = new HashSet<>();
        return reqs.stream().map(p -> {
            requireText(p.getName(), 50, "이름(한글)");
            requireText(p.getNameEn(), 50, "이름(영문)");
            requireText(p.getEmail(), 64, "이메일");
            requireText(p.getMobile(), 32, "전화번호");
            if (!EMAIL_PATTERN.matcher(p.getEmail().trim()).matches()) {
                throw business("이메일 형식이 올바르지 않습니다.", HttpsStatusCode.INVALID_PARAMETER);
            }
            if (!MOBILE_PATTERN.matcher(p.getMobile().trim()).matches()) {
                throw business("전화번호 형식이 올바르지 않습니다.", HttpsStatusCode.INVALID_PARAMETER);
            }
            if (p.getGender() == null || !GENDERS.contains(p.getGender())) {
                throw business("성별이 올바르지 않습니다.", HttpsStatusCode.INVALID_PARAMETER);
            }
            try {
                if (LocalDate.parse(p.getBirthDate()).isAfter(LocalDate.now())) {
                    throw business("생년월일이 올바르지 않습니다.", HttpsStatusCode.INVALID_PARAMETER);
                }
            } catch (DateTimeParseException | NullPointerException e) {
                throw business("생년월일 형식이 올바르지 않습니다.", HttpsStatusCode.INVALID_PARAMETER);
            }
            if (p.getSeatNumber() == null || !SEAT_PATTERN.matcher(p.getSeatNumber()).matches() || !seats.add(p.getSeatNumber())) {
                throw business("좌석 정보가 올바르지 않습니다.", HttpsStatusCode.INVALID_PARAMETER);
            }

            return BookingSessionDto.Passenger.builder()
                    .name(p.getName().trim())
                    .nameEn(p.getNameEn().trim())
                    .email(p.getEmail().trim())
                    .mobile(p.getMobile().trim())
                    .birthDate(p.getBirthDate())
                    .gender(p.getGender())
                    .seatNumber(p.getSeatNumber())
                    .build();
        }).toList();
    }

    private void requireText(String value, int maxLength, String label) {
        if (value == null || value.isBlank()) {
            throw business(label + "을(를) 입력해주세요.", HttpsStatusCode.INVALID_PARAMETER);
        }
        if (value.trim().length() > maxLength) {
            throw business(label + "은(는) " + maxLength + "자 이하로 입력해주세요.", HttpsStatusCode.INVALID_PARAMETER);
        }
    }

    private BookingSessionDto.Session findSession(String sessionId, Long memberId) {
        return bookingSessionService.findOwnedSession(sessionId, memberId)
                .orElseThrow(() -> business("예약 가능 시간이 지나 좌석 및 운임 정보가 변경되었을 수 있습니다.", HttpsStatusCode.REQUEST_TIMEOUT));
    }

    private PaymentDto.ConfirmRes toConfirmRes(Booking booking) {
        return PaymentDto.ConfirmRes.builder()
                .bookingId(booking.getId())
                .bookingNumber(booking.getBookingNumber())
                .build();
    }

    private Long currentMemberId() {
        Long memberId = SecurityUtil.getCurrentMemberId();
        if (memberId == null) {
            throw business(HttpsStatusCode.LOGIN_REQUIRED);
        }
        return memberId;
    }

}
