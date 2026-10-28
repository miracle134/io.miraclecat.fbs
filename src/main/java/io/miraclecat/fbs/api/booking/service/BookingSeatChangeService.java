/**
 * packageName    : io.miraclecat.fbs.api.booking.service
 * fileName       : BookingSeatChangeService
 * author         : MiracleCat
 * date           : 2026-10-25
 * description    : 좌석 변경 흐름 (트랜잭션 없음)
 *                  - 출발 하루 전까지, 여러 탑승객 한 번에, 수수료 없음
 *                  - 새 좌석은 본인 선점 필수 (내 예약 안에서 맞바꾸는 좌석은 제외)
 *                  - 커밋 후 선점 해제, 새 좌석 BOOKED / 비워진 좌석 RELEASED 전파
 */
package io.miraclecat.fbs.api.booking.service;

import io.miraclecat.fbs.api.booking.domain.entity.Booking;
import io.miraclecat.fbs.api.booking.domain.entity.BookingPassenger;
import io.miraclecat.fbs.api.booking.domain.repository.BookingPassengerRepository;
import io.miraclecat.fbs.api.booking.domain.repository.BookingRepository;
import io.miraclecat.fbs.api.booking.dto.BookingDto;
import io.miraclecat.fbs.api.flight.domain.entity.FlightInventory;
import io.miraclecat.fbs.api.flight.domain.repository.FlightInventoryRepository;
import io.miraclecat.fbs.api.flight.dto.SeatDto;
import io.miraclecat.fbs.api.flight.redis.SeatHoldStore;
import io.miraclecat.fbs.api.flight.websocket.SeatEventPublisher;
import io.miraclecat.fbs.core.response.ApiEntity;
import io.miraclecat.fbs.core.response.HttpsStatusCode;
import io.miraclecat.fbs.core.utils.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static io.miraclecat.fbs.core.exception.GlobalException.business;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingSeatChangeService {

    private static final Pattern SEAT_PATTERN = Pattern.compile("^[1-9]\\d*[A-F]$");

    private final BookingRepository bookingRepository;
    private final BookingPassengerRepository bookingPassengerRepository;
    private final FlightInventoryRepository flightInventoryRepository;
    private final SeatHoldStore seatHoldStore;
    private final SeatEventPublisher seatEventPublisher;
    private final BookingSeatChangeTxService bookingSeatChangeTxService;

    public ApiEntity<Void> changeSeats(Long bookingId, BookingDto.SeatChangeReq req) {
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
            throw business("취소된 예약은 좌석을 변경할 수 없습니다.", HttpsStatusCode.UNPROCESSABLE_ENTITY);
        }
        if (!booking.isChangeableBy(LocalDate.now())) {
            throw business("출발 하루 전까지만 좌석을 변경할 수 있습니다.", HttpsStatusCode.UNPROCESSABLE_ENTITY);
        }
        if (req == null || req.getChanges() == null || req.getChanges().isEmpty()) {
            throw business(HttpsStatusCode.INVALID_PARAMETER);
        }

        FlightInventory inventory = flightInventoryRepository
                .findByFlightIdAndFlightDate(booking.getFlightId(), booking.getFlightDate())
                .orElseThrow(() -> business(HttpsStatusCode.RESOURCE_NOT_FOUND));

        Map<Long, String> current = bookingPassengerRepository.findByBookingIdOrderByIdAsc(bookingId).stream()
                .collect(Collectors.toMap(BookingPassenger::getId, BookingPassenger::getSeatNumber, (a, b) -> a, LinkedHashMap::new));

        Map<Long, String> changes = new LinkedHashMap<>();
        for (BookingDto.SeatChangeItem item : req.getChanges()) {
            if (item.getPassengerId() == null || !current.containsKey(item.getPassengerId())) {
                throw business("탑승객 정보가 올바르지 않습니다.", HttpsStatusCode.INVALID_PARAMETER);
            }
            if (item.getSeatNumber() == null || !SEAT_PATTERN.matcher(item.getSeatNumber()).matches()) {
                throw business("좌석 번호 형식이 올바르지 않습니다.", HttpsStatusCode.INVALID_PARAMETER);
            }
            if (!item.getSeatNumber().equals(current.get(item.getPassengerId()))) {
                changes.put(item.getPassengerId(), item.getSeatNumber());
            }
        }
        if (changes.isEmpty()) {
            return ApiEntity.of(HttpsStatusCode.SUCCESS);
        }

        Map<Long, String> finalSeats = new HashMap<>(current);
        finalSeats.putAll(changes);
        if (new HashSet<>(finalSeats.values()).size() != finalSeats.size()) {
            throw business("탑승객 간 좌석이 중복됩니다.", HttpsStatusCode.INVALID_PARAMETER);
        }

        Set<String> ownSeats = new HashSet<>(current.values());
        List<String> newSeats = changes.values().stream().filter(seat -> !ownSeats.contains(seat)).toList();
        validateMyHolds(inventory.getId(), memberId, newSeats);

        try {
            bookingSeatChangeTxService.changeSeats(bookingId, inventory.getId(), changes, memberId);
        } catch (DataIntegrityViolationException e) {
            log.warn("[좌석 변경 실패: 좌석 중복] bookingNumber={}, seats={}", booking.getBookingNumber(), newSeats);
            throw business("선택한 좌석이 이미 예약되었습니다. 좌석을 다시 선택해주세요.", HttpsStatusCode.DUPLICATE_ITEM);
        }

        Set<String> finalSet = new HashSet<>(finalSeats.values());
        List<String> released = changes.keySet().stream()
                .map(current::get)
                .filter(seat -> !finalSet.contains(seat))
                .toList();
        afterChanged(inventory.getId(), memberId, newSeats, released, req.getClientId());
        return ApiEntity.of(HttpsStatusCode.SUCCESS);
    }

    private void validateMyHolds(Long inventoryId, Long memberId, List<String> seats) {
        if (seats.isEmpty()) return;
        Map<String, String> holds = seatHoldStore.findActiveHolds(inventoryId);
        String me = String.valueOf(memberId);
        List<String> lost = seats.stream().filter(seat -> !me.equals(holds.get(seat))).toList();
        if (!lost.isEmpty()) {
            throw business("선택한 좌석(" + String.join(", ", lost) + ")의 선점이 만료되었습니다. 좌석을 다시 선택해주세요.",
                    HttpsStatusCode.DUPLICATE_ITEM);
        }
    }

    private void afterChanged(Long inventoryId, Long memberId, List<String> booked, List<String> released, String clientId) {
        try {
            booked.forEach(seat -> seatHoldStore.release(inventoryId, seat, memberId));
            seatEventPublisher.publish(SeatDto.EventType.BOOKED, inventoryId, booked, clientId);
            seatEventPublisher.publish(SeatDto.EventType.RELEASED, inventoryId, released, clientId);
        } catch (Exception e) {
            log.warn("[좌석 변경 후 정리 실패] inventoryId={}, cause={}", inventoryId, e.getMessage());
        }
    }

}
