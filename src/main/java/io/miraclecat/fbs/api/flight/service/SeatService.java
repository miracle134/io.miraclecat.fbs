/**
 * packageName    : io.miraclecat.fbs.api.flight.service
 * fileName       : SeatService
 * author         : MiracleCat
 * date           : 2026-10-25
 * description    : 좌석 서비스 (좌석 상태 조회 / 임시 선점·해제)
 */
package io.miraclecat.fbs.api.flight.service;

import io.miraclecat.fbs.api.flight.domain.entity.Flight;
import io.miraclecat.fbs.api.flight.domain.entity.FlightInventory;
import io.miraclecat.fbs.api.flight.domain.repository.FlightInventoryRepository;
import io.miraclecat.fbs.api.flight.domain.repository.FlightRepository;
import io.miraclecat.fbs.api.flight.domain.repository.SeatReservationRepository;
import io.miraclecat.fbs.api.flight.dto.SeatDto;
import io.miraclecat.fbs.api.flight.redis.SeatHoldStore;
import io.miraclecat.fbs.api.flight.websocket.SeatEventPublisher;
import io.miraclecat.fbs.core.config.ReadTx;
import io.miraclecat.fbs.core.response.ApiEntity;
import io.miraclecat.fbs.core.response.HttpsStatusCode;
import io.miraclecat.fbs.core.utils.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.miraclecat.fbs.core.exception.GlobalException.business;

@Service
@RequiredArgsConstructor
public class SeatService {

    private static final Pattern SEAT_PATTERN = Pattern.compile("^([1-9]\\d*)([A-F])$");
    private static final int SEATS_PER_ROW = 6;
    private static final int MAX_HOLDS_PER_MEMBER = 9;

    private final FlightInventoryRepository flightInventoryRepository;
    private final FlightRepository flightRepository;
    private final SeatReservationRepository seatReservationRepository;
    private final SeatHoldStore seatHoldStore;
    private final SeatEventPublisher seatEventPublisher;

    /**
     * 좌석 상태 조회 (예약 확정 + 임시 선점)
     */
    @ReadTx
    public ApiEntity<SeatDto.StatusRes> getSeatStatus(Long inventoryId) {
        FlightInventory inventory = findInventory(inventoryId);
        Flight flight = findFlight(inventory.getFlightId());

        List<String> booked = seatReservationRepository.findSeatNumbersByInventoryId(inventoryId);
        Set<String> bookedSet = new HashSet<>(booked);

        Long memberId = SecurityUtil.getCurrentMemberId();
        String me = memberId != null ? String.valueOf(memberId) : null;

        List<String> held = new ArrayList<>();
        List<String> myHeld = new ArrayList<>();
        for (Map.Entry<String, String> hold : seatHoldStore.findActiveHolds(inventoryId).entrySet()) {
            if (bookedSet.contains(hold.getKey())) continue;
            if (hold.getValue().equals(me)) {
                myHeld.add(hold.getKey());
            } else {
                held.add(hold.getKey());
            }
        }

        SeatDto.StatusRes res = SeatDto.StatusRes.builder()
                .inventoryId(inventoryId)
                .totalSeats(flight.getTotalSeats())
                .availableSeats(inventory.getAvailableSeats())
                .booked(booked)
                .held(held)
                .myHeld(myHeld)
                .build();
        return ApiEntity.of(HttpsStatusCode.SUCCESS, res);
    }

    /**
     * 좌석 임시 선점
     */
    @ReadTx
    public ApiEntity<SeatDto.HoldRes> holdSeat(Long inventoryId, String seatNumber, SeatDto.HoldReq req) {
        Long memberId = currentMemberId();

        FlightInventory inventory = findInventory(inventoryId);
        if (inventory.getFlightDate().isBefore(LocalDate.now())) {
            throw business("이미 출발한 항공편입니다.", HttpsStatusCode.UNPROCESSABLE_ENTITY);
        }
        Flight flight = findFlight(inventory.getFlightId());
        validateSeatNumber(seatNumber, flight.getTotalSeats());

        if (seatReservationRepository.existsByInventoryIdAndSeatNumber(inventoryId, seatNumber)) {
            throw business("이미 예약된 좌석입니다.", HttpsStatusCode.DUPLICATE_ITEM);
        }

        Map<String, String> holds = seatHoldStore.findActiveHolds(inventoryId);
        String me = String.valueOf(memberId);
        long myHoldCount = holds.values().stream().filter(me::equals).count();
        if (!me.equals(holds.get(seatNumber)) && myHoldCount >= MAX_HOLDS_PER_MEMBER) {
            throw business("좌석은 최대 " + MAX_HOLDS_PER_MEMBER + "석까지 선택할 수 있습니다.", HttpsStatusCode.UNPROCESSABLE_ENTITY);
        }

        SeatHoldStore.HoldResult result = seatHoldStore.hold(inventoryId, seatNumber, memberId);
        if (result == SeatHoldStore.HoldResult.FAILED) {
            throw business("다른 사용자가 선택 중인 좌석입니다.", HttpsStatusCode.DUPLICATE_ITEM);
        }

        if (result == SeatHoldStore.HoldResult.HELD) {
            seatEventPublisher.publish(SeatDto.EventType.HELD, inventoryId, List.of(seatNumber), req.getClientId());
        }

        SeatDto.HoldRes res = SeatDto.HoldRes.builder()
                .seatNumber(seatNumber)
                .expiresAt(System.currentTimeMillis() + seatHoldStore.getTtlSeconds() * 1000)
                .build();
        return ApiEntity.of(HttpsStatusCode.SUCCESS, res);
    }

    /**
     * 내 선점 일괄 연장 (예약 화면이 열려 있는 동안 주기적으로 호출)
     */
    public ApiEntity<SeatDto.ExtendRes> extendHolds(Long inventoryId) {
        Long memberId = currentMemberId();
        List<String> seats = seatHoldStore.extendAll(inventoryId, memberId);

        SeatDto.ExtendRes res = SeatDto.ExtendRes.builder()
                .seats(seats)
                .expiresAt(System.currentTimeMillis() + seatHoldStore.getTtlSeconds() * 1000)
                .build();
        return ApiEntity.of(HttpsStatusCode.SUCCESS, res);
    }

    /**
     * 좌석 임시 선점 해제 (본인 선점만, 이미 없으면 그대로 성공)
     */
    public ApiEntity<Void> releaseSeat(Long inventoryId, String seatNumber, SeatDto.HoldReq req) {
        Long memberId = currentMemberId();

        if (seatHoldStore.release(inventoryId, seatNumber, memberId)) {
            seatEventPublisher.publish(SeatDto.EventType.RELEASED, inventoryId, List.of(seatNumber), req.getClientId());
        }
        return ApiEntity.of(HttpsStatusCode.SUCCESS);
    }

    private Long currentMemberId() {
        Long memberId = SecurityUtil.getCurrentMemberId();
        if (memberId == null) {
            throw business(HttpsStatusCode.LOGIN_REQUIRED);
        }
        return memberId;
    }

    /**
     * 좌석 번호 형식(예: 12A)과 항공편 좌석 범위 검증
     */
    private void validateSeatNumber(String seatNumber, int totalSeats) {
        Matcher matcher = seatNumber != null ? SEAT_PATTERN.matcher(seatNumber) : null;
        if (matcher == null || !matcher.matches()) {
            throw business("좌석 번호 형식이 올바르지 않습니다.", HttpsStatusCode.INVALID_PARAMETER);
        }
        int row = Integer.parseInt(matcher.group(1));
        int column = matcher.group(2).charAt(0) - 'A';
        if ((row - 1) * SEATS_PER_ROW + column >= totalSeats) {
            throw business("존재하지 않는 좌석입니다.", HttpsStatusCode.INVALID_PARAMETER);
        }
    }

    private FlightInventory findInventory(Long inventoryId) {
        return flightInventoryRepository.findById(inventoryId)
                .orElseThrow(() -> business(HttpsStatusCode.RESOURCE_NOT_FOUND));
    }

    private Flight findFlight(Long flightId) {
        return flightRepository.findById(flightId)
                .orElseThrow(() -> business(HttpsStatusCode.RESOURCE_NOT_FOUND));
    }

}
