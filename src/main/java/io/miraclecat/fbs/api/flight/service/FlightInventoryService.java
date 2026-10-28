/**
 * packageName    : io.miraclecat.fbs.api.flight.service
 * fileName       : FlightInventoryService
 * author         : MiracleCat
 * date           : 2026-10-25
 * description    : 항공편 재고 변경 서비스 (재고 변경 + 재고 로그 저장)
 *                  - 예약/취소: 원자적 UPDATE (동시 결제 시 재고 행 충돌 없음)
 *                  - 관리자 조정: @Version 엔티티 수정 (동시 변경 시 충돌로 감지)
 */
package io.miraclecat.fbs.api.flight.service;

import io.miraclecat.fbs.api.flight.domain.entity.Flight;
import io.miraclecat.fbs.api.flight.domain.entity.FlightInventory;
import io.miraclecat.fbs.api.flight.domain.entity.InventoryLog;
import io.miraclecat.fbs.api.flight.domain.repository.FlightInventoryRepository;
import io.miraclecat.fbs.api.flight.domain.repository.FlightRepository;
import io.miraclecat.fbs.api.flight.domain.repository.InventoryLogRepository;
import io.miraclecat.fbs.api.flight.domain.repository.SeatReservationRepository;
import io.miraclecat.fbs.api.flight.dto.FlightInventoryDto;
import io.miraclecat.fbs.core.config.RequiredTx;
import io.miraclecat.fbs.core.response.HttpsStatusCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static io.miraclecat.fbs.core.exception.GlobalException.business;

@Service
@RequiredArgsConstructor
public class FlightInventoryService {

    private final FlightInventoryRepository flightInventoryRepository;
    private final InventoryLogRepository inventoryLogRepository;
    private final FlightRepository flightRepository;
    private final SeatReservationRepository seatReservationRepository;

    /**
     * 재고 차감 (예약 확정) — 원자적 UPDATE, 남은 좌석이 부족하면 갱신 0건 → 예외
     */
    @RequiredTx
    public void reserve(Long inventoryId, int count, Long bookingId) {
        FlightInventory inventory = findById(inventoryId);

        int updated = flightInventoryRepository.decreaseSeats(inventoryId, count, LocalDateTime.now());
        if (updated == 0) {
            throw business("남은 좌석이 부족합니다.", HttpsStatusCode.UNPROCESSABLE_ENTITY);
        }

        int afterSeats = flightInventoryRepository.findAvailableSeats(inventoryId);
        saveLog(inventory, InventoryLog.TYPE_RESERVE, afterSeats + count, afterSeats, bookingId);
    }

    /**
     * 재고 복원 (예약 취소) — 원자적 UPDATE
     */
    @RequiredTx
    public void release(Long flightId, LocalDate flightDate, int count, Long bookingId) {
        FlightInventory inventory = flightInventoryRepository.findByFlightIdAndFlightDate(flightId, flightDate)
                .orElseThrow(() -> business(HttpsStatusCode.RESOURCE_NOT_FOUND));

        flightInventoryRepository.increaseSeats(inventory.getId(), count, LocalDateTime.now());

        int afterSeats = flightInventoryRepository.findAvailableSeats(inventory.getId());
        saveLog(inventory, InventoryLog.TYPE_RELEASE, afterSeats - count, afterSeats, bookingId);
    }

    /**
     * 재고 수동 조정 (관리자, 판매 가능 좌석 수)
     * - 오늘 이후 운항일만
     * - 0 ≤ 판매 가능 좌석 ≤ 총 좌석 - 확정 예약 좌석
     */
    @RequiredTx
    public FlightInventory adjust(Long inventoryId, FlightInventoryDto.AdjustReq req) {
        FlightInventory inventory = findById(inventoryId);
        if (inventory.getFlightDate().isBefore(LocalDate.now())) {
            throw business("지난 운항일의 재고는 조정할 수 없습니다.", HttpsStatusCode.UNPROCESSABLE_ENTITY);
        }

        Flight flight = flightRepository.findById(inventory.getFlightId())
                .orElseThrow(() -> business(HttpsStatusCode.RESOURCE_NOT_FOUND));
        long booked = seatReservationRepository.countByInventoryId(inventoryId);
        long max = flight.getTotalSeats() - booked;
        Integer seats = req.getAvailableSeats();
        if (seats == null || seats < 0 || seats > max) {
            throw business("판매 가능 좌석은 0 ~ " + max + "석 사이로 입력해주세요. (확정 예약 " + booked + "석)",
                    HttpsStatusCode.INVALID_PARAMETER);
        }

        int beforeSeats = inventory.getAvailableSeats();

        inventory.adjustSeats(req);
        saveLog(inventory, InventoryLog.TYPE_ADJUST, beforeSeats, inventory.getAvailableSeats(), null);
        return inventory;
    }

    private FlightInventory findById(Long inventoryId) {
        return flightInventoryRepository.findById(inventoryId)
                .orElseThrow(() -> business(HttpsStatusCode.RESOURCE_NOT_FOUND));
    }

    private void saveLog(FlightInventory inventory, String changeType, int beforeSeats, int afterSeats, Long bookingId) {
        InventoryLog log = InventoryLog.builder()
                .flightId(inventory.getFlightId())
                .flightDate(inventory.getFlightDate())
                .changeType(changeType)
                .beforeSeats(beforeSeats)
                .afterSeats(afterSeats)
                .bookingId(bookingId)
                .build();
        inventoryLogRepository.save(log);
    }

}
