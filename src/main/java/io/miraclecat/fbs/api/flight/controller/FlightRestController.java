/**
 * packageName    : io.miraclecat.fbs.api.flight.controller
 * fileName       : FlightRestController
 * author         : MiracleCat
 * date           : 2026-05-19
 * description    : 항공편 컨트롤러
 */
package io.miraclecat.fbs.api.flight.controller;

import io.miraclecat.fbs.api.flight.dto.FlightDto;
import io.miraclecat.fbs.api.flight.dto.FlightInventoryDto;
import io.miraclecat.fbs.api.flight.dto.SeatDto;
import io.miraclecat.fbs.api.flight.service.FlightService;
import io.miraclecat.fbs.api.flight.service.SeatService;
import io.miraclecat.fbs.core.response.ApiEntity;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
public class FlightRestController {

    private final FlightService flightService;
    private final SeatService seatService;

    /**
     * 항공편 검색
     * @param req req
     * @return ApiEntity
     */
    @GetMapping("/api/flight/search")
    public ApiEntity<?> getFlightsSearch(@ModelAttribute FlightDto.SearchReq req) {
        return flightService.getFlightsSearch(req);
    }

    /**
     * 항공편 상세 조회
     * @param flightId flightId
     * @return ApiEntity
     */
    @GetMapping("/api/flight/{flight-id}")
    public ApiEntity<?> getFlightDetail(@PathVariable("flight-id") Long flightId) {
        return flightService.getFlightDetail(flightId);
    }

    /**
     * 재고 상세 조회
     * @param inventoryId inventoryId
     * @return ApiEntity
     */
    @GetMapping("/api/flight/inventory/{inventory-id}")
    public ApiEntity<?> getInventoryDetail(@PathVariable("inventory-id") Long inventoryId) {
        return flightService.getInventoryDetail(inventoryId);
    }

    /**
     * 좌석 상태 조회 (예약 확정 + 임시 선점)
     * @param inventoryId inventoryId
     * @return ApiEntity
     */
    @GetMapping("/api/flight/inventory/{inventory-id}/seats")
    public ApiEntity<?> getSeatStatus(@PathVariable("inventory-id") Long inventoryId) {
        return seatService.getSeatStatus(inventoryId);
    }

    /**
     * 좌석 임시 선점
     * @param inventoryId inventoryId
     * @param seatNumber seatNumber
     * @param req req
     * @return ApiEntity
     */
    @PostMapping("/api/flight/inventory/{inventory-id}/seats/{seat-number}/hold")
    public ApiEntity<?> holdSeat(@PathVariable("inventory-id") Long inventoryId,
                                 @PathVariable("seat-number") String seatNumber,
                                 @RequestBody SeatDto.HoldReq req) {
        return seatService.holdSeat(inventoryId, seatNumber, req);
    }

    /**
     * 내 좌석 선점 일괄 연장
     * @param inventoryId inventoryId
     * @return ApiEntity
     */
    @PostMapping("/api/flight/inventory/{inventory-id}/seats/hold/extend")
    public ApiEntity<?> extendSeatHolds(@PathVariable("inventory-id") Long inventoryId) {
        return seatService.extendHolds(inventoryId);
    }

    /**
     * 좌석 임시 선점 해제
     * @param inventoryId inventoryId
     * @param seatNumber seatNumber
     * @param req req
     * @return ApiEntity
     */
    @DeleteMapping("/api/flight/inventory/{inventory-id}/seats/{seat-number}/hold")
    public ApiEntity<?> releaseSeat(@PathVariable("inventory-id") Long inventoryId,
                                    @PathVariable("seat-number") String seatNumber,
                                    @RequestBody SeatDto.HoldReq req) {
        return seatService.releaseSeat(inventoryId, seatNumber, req);
    }

    /**
     * 항공편 목록 (관리자)
     */
    @GetMapping("/api/admin/flight")
    public ApiEntity<?> getAdminFlights(@ModelAttribute FlightDto.SearchReq req) {
        return flightService.getFlights(req);
    }

    /**
     * 항공편 상세 (관리자, 재고 현황 포함)
     */
    @GetMapping("/api/admin/flight/{flight-id}")
    public ApiEntity<?> getAdminFlightDetail(@PathVariable("flight-id") Long flightId) {
        return flightService.getAdminFlightDetail(flightId);
    }

    /**
     * 항공편 등록 (관리자)
     */
    @PostMapping("/api/admin/flight")
    public ApiEntity<?> createFlight(@RequestBody FlightDto.CreateReq req) {
        return flightService.createFlight(req);
    }

    /**
     * 항공편 수정 (관리자)
     */
    @PutMapping("/api/admin/flight/{flight-id}")
    public ApiEntity<?> updateFlight(@PathVariable("flight-id") Long flightId,
                                     @RequestBody FlightDto.UpdateReq req) {
        return flightService.updateFlight(flightId, req);
    }

    /**
     * 항공편 운항 중지 (관리자)
     */
    @DeleteMapping("/api/admin/flight/{flight-id}")
    public ApiEntity<?> deleteFlight(@PathVariable("flight-id") Long flightId) {
        return flightService.deleteFlight(flightId);
    }

    /**
     * 재고 수동 조정 (관리자)
     */
    @PutMapping("/api/admin/flight/inventory/{inventory-id}")
    public ApiEntity<?> adjustInventory(@PathVariable("inventory-id") Long inventoryId,
                                        @RequestBody FlightInventoryDto.AdjustReq req) {
        return flightService.adjustInventory(inventoryId, req);
    }

    /**
     * 재고 일괄 생성 (관리자)
     */
    @PostMapping("/api/admin/flight/{flight-id}/inventory/generate")
    public ApiEntity<?> generateInventory(@PathVariable("flight-id") Long flightId,
                                          @RequestBody FlightInventoryDto.GenerateReq req) {
        return flightService.generateInventory(flightId, req);
    }

}
