/**
 * packageName    : io.miraclecat.fbs.api.booking.service
 * fileName       : BookingSessionService
 * author         : MiracleCat
 * date           : 2026-10-25
 * description    : 예약 세션 서비스
 *                  - 검색 목록 → 예약 화면 진입 시 생성 (15분)
 *                  - 결제하기 시 max(현재 만료, 지금+10분), 단 생성 후 25분 상한
 *                  - 금액(항공권 + 1인당 예약 수수료)을 생성 시점에 계산해 저장 → 결제 금액 검증 기준
 */
package io.miraclecat.fbs.api.booking.service;

import io.miraclecat.fbs.api.admin.service.SettingQueryService;
import io.miraclecat.fbs.api.booking.dto.BookingSessionDto;
import io.miraclecat.fbs.api.booking.redis.BookingSessionStore;
import io.miraclecat.fbs.api.flight.domain.entity.Flight;
import io.miraclecat.fbs.api.flight.domain.entity.FlightInventory;
import io.miraclecat.fbs.api.flight.domain.repository.FlightInventoryRepository;
import io.miraclecat.fbs.api.flight.domain.repository.FlightRepository;
import io.miraclecat.fbs.core.config.ReadTx;
import io.miraclecat.fbs.core.response.ApiEntity;
import io.miraclecat.fbs.core.response.HttpsStatusCode;
import io.miraclecat.fbs.core.utils.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

import static io.miraclecat.fbs.core.exception.GlobalException.business;

@Service
@RequiredArgsConstructor
public class BookingSessionService {

    public static final long SESSION_MS = Duration.ofMinutes(15).toMillis();
    public static final long PAYMENT_MS = Duration.ofMinutes(10).toMillis();
    public static final long MAX_SESSION_MS = Duration.ofMinutes(25).toMillis();
    public static final long MIN_PAYMENT_MS = Duration.ofMinutes(3).toMillis();

    private static final int MAX_PASSENGERS = 9;
    private static final int DEFAULT_BOOKING_FEE = 5000;
    private static final String FLIGHT_ACTIVE = "active";
    private static final DateTimeFormatter LIST_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final BookingSessionStore bookingSessionStore;
    private final FlightInventoryRepository flightInventoryRepository;
    private final FlightRepository flightRepository;
    private final SettingQueryService settingQueryService;

    /**
     * 예약 세션 생성
     */
    @ReadTx
    public ApiEntity<BookingSessionDto.Res> createSession(BookingSessionDto.CreateReq req) {
        Long memberId = currentMemberId();

        Integer count = req.getPassengerCount();
        if (req.getInventoryId() == null || count == null || count < 1 || count > MAX_PASSENGERS) {
            throw business(HttpsStatusCode.INVALID_PARAMETER);
        }

        FlightInventory inventory = flightInventoryRepository.findById(req.getInventoryId())
                .orElseThrow(() -> business(HttpsStatusCode.RESOURCE_NOT_FOUND));
        Flight flight = flightRepository.findById(inventory.getFlightId())
                .orElseThrow(() -> business(HttpsStatusCode.RESOURCE_NOT_FOUND));

        if (!FLIGHT_ACTIVE.equals(flight.getStatus())) {
            throw business("예약할 수 없는 항공편입니다.", HttpsStatusCode.UNPROCESSABLE_ENTITY);
        }
        if (!inventory.getFlightDate().isAfter(LocalDate.now())) {
            throw business("당일 또는 지난 날짜의 항공편은 예약할 수 없습니다.", HttpsStatusCode.UNPROCESSABLE_ENTITY);
        }
        if (inventory.getAvailableSeats() < count) {
            throw business("남은 좌석이 부족합니다.", HttpsStatusCode.UNPROCESSABLE_ENTITY);
        }

        int bookingFee = settingQueryService.getInt(SettingQueryService.BOOKING_FEE_PER_PERSON, DEFAULT_BOOKING_FEE);
        int fareAmount = flight.getBasePrice() * count;
        int bookingFeeAmount = bookingFee * count;
        long now = System.currentTimeMillis();

        BookingSessionDto.Session session = BookingSessionDto.Session.builder()
                .sessionId(UUID.randomUUID().toString())
                .memberId(memberId)
                .flightId(flight.getId())
                .inventoryId(inventory.getId())
                .flightDate(inventory.getFlightDate().toString())
                .departureAirport(flight.getDepartureAirport())
                .arrivalAirport(flight.getArrivalAirport())
                .passengerCount(count)
                .basePrice(flight.getBasePrice())
                .fareAmount(fareAmount)
                .bookingFeeAmount(bookingFeeAmount)
                .totalAmount(fareAmount + bookingFeeAmount)
                .createdAt(now)
                .expireAt(now + SESSION_MS)
                .maxExpireAt(now + MAX_SESSION_MS)
                .build();
        bookingSessionStore.save(session);

        return ApiEntity.of(HttpsStatusCode.REGISTER, toRes(session));
    }

    /**
     * 예약 세션 조회 (본인 세션만)
     */
    public ApiEntity<BookingSessionDto.Res> getSession(String sessionId) {
        BookingSessionDto.Session session = findOwnedSession(sessionId, currentMemberId())
                .orElseThrow(() -> business("예약 가능 시간이 만료되었습니다.", HttpsStatusCode.RESOURCE_NOT_FOUND));
        return ApiEntity.of(HttpsStatusCode.SUCCESS, toRes(session));
    }

    /**
     * 예약 세션 삭제 (본인 세션만, 없으면 그대로 성공)
     */
    public ApiEntity<Void> deleteSession(String sessionId) {
        Long memberId = currentMemberId();
        findOwnedSession(sessionId, memberId).ifPresent(s -> bookingSessionStore.delete(sessionId));
        return ApiEntity.of(HttpsStatusCode.SUCCESS);
    }

    /**
     * 유효한 본인 세션 조회 (뷰 컨트롤러 / 결제에서 사용)
     */
    public Optional<BookingSessionDto.Session> findOwnedSession(String sessionId, Long memberId) {
        if (memberId == null) return Optional.empty();
        return bookingSessionStore.find(sessionId)
                .filter(s -> memberId.equals(s.getMemberId()))
                .filter(s -> s.getExpireAt() > System.currentTimeMillis());
    }

    /**
     * 결제하기 시 세션 연장: max(현재 만료, 지금+10분), 생성 후 25분 상한
     * 연장 후 남은 시간이 3분 미만이면 결제 시작 불가 (REQUEST_TIMEOUT → 화면에서 재조회 처리)
     */
    public BookingSessionDto.Session extendForPayment(BookingSessionDto.Session session) {
        long now = System.currentTimeMillis();
        long expireAt = Math.min(Math.max(session.getExpireAt(), now + PAYMENT_MS), session.getMaxExpireAt());
        if (expireAt - now < MIN_PAYMENT_MS) {
            throw business("예약 가능 시간이 지나 좌석 및 운임 정보가 변경되었을 수 있습니다.", HttpsStatusCode.REQUEST_TIMEOUT);
        }
        session.setExpireAt(expireAt);
        bookingSessionStore.save(session);
        return session;
    }

    public void save(BookingSessionDto.Session session) {
        bookingSessionStore.save(session);
    }

    public void delete(String sessionId) {
        bookingSessionStore.delete(sessionId);
    }

    /**
     * 만료 시 이동할 같은 조건의 항공편 목록 URL
     */
    public String listUrl(BookingSessionDto.Session session) {
        LocalDate flightDate = LocalDate.parse(session.getFlightDate());
        if (!flightDate.isAfter(LocalDate.now())) {
            return "/";
        }
        return "/flight/flight-list/" + session.getDepartureAirport() + "/" + session.getArrivalAirport()
                + "/" + flightDate.format(LIST_DATE) + "/" + session.getPassengerCount();
    }

    private BookingSessionDto.Res toRes(BookingSessionDto.Session session) {
        return BookingSessionDto.Res.builder()
                .sessionId(session.getSessionId())
                .flightId(session.getFlightId())
                .inventoryId(session.getInventoryId())
                .flightDate(session.getFlightDate())
                .departureAirport(session.getDepartureAirport())
                .arrivalAirport(session.getArrivalAirport())
                .passengerCount(session.getPassengerCount())
                .basePrice(session.getBasePrice())
                .fareAmount(session.getFareAmount())
                .bookingFeeAmount(session.getBookingFeeAmount())
                .totalAmount(session.getTotalAmount())
                .expireAt(session.getExpireAt())
                .serverTime(System.currentTimeMillis())
                .listUrl(listUrl(session))
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
