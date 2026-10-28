/**
 * packageName    : io.miraclecat.fbs.api.flight.service
 * fileName       : FlightService
 * author         : MiracleCat
 * date           : 2026-05-19
 * description    : 항공편 서비스
 *                  - 사용자: 검색 / 상세 / 재고 조회
 *                  - 관리자: 항공편 등록·수정·운항중지, 재고 일괄 생성·조정
 */
package io.miraclecat.fbs.api.flight.service;

import io.miraclecat.fbs.api.code.domain.repository.AirlineRepository;
import io.miraclecat.fbs.api.code.domain.repository.AirportRepository;
import io.miraclecat.fbs.api.flight.dao.FlightDao;
import io.miraclecat.fbs.api.flight.domain.entity.Flight;
import io.miraclecat.fbs.api.flight.domain.entity.FlightInventory;
import io.miraclecat.fbs.api.flight.domain.repository.FlightInventoryRepository;
import io.miraclecat.fbs.api.flight.domain.repository.FlightRepository;
import io.miraclecat.fbs.api.flight.dto.FlightDto;
import io.miraclecat.fbs.api.flight.dto.FlightInventoryDto;
import io.miraclecat.fbs.core.config.ReadTx;
import io.miraclecat.fbs.core.config.RequiredTx;
import io.miraclecat.fbs.core.response.ApiEntity;
import io.miraclecat.fbs.core.response.HttpsStatusCode;
import io.miraclecat.fbs.core.utils.persistence.PageRes;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import static io.miraclecat.fbs.core.exception.GlobalException.business;

@Service
@RequiredArgsConstructor
public class FlightService {

    private static final Pattern FLIGHT_NUMBER = Pattern.compile("^[A-Z0-9]{2}\\d{1,4}$");
    private static final Set<String> STATUSES = Set.of(Flight.STATUS_ACTIVE, Flight.STATUS_INACTIVE);
    private static final int MIN_SEATS = 6;
    private static final int MAX_SEATS = 300;
    private static final int MAX_GENERATE_DAYS = 90;
    private static final int UPCOMING_LIMIT = 14;

    private final FlightRepository flightRepository;
    private final FlightInventoryRepository flightInventoryRepository;
    private final AirlineRepository airlineRepository;
    private final AirportRepository airportRepository;
    private final FlightDao flightDao;
    private final FlightInventoryService flightInventoryService;

    /**
     * 항공편 검색 (사용자, 운항 중 항공편만)
     */
    @ReadTx
    public ApiEntity<PageRes<FlightDto.SearchRes>> getFlightsSearch(FlightDto.SearchReq req) {
        PageRes<FlightDto.SearchRes> flights = flightDao.searchFlights(req);
        return ApiEntity.of(HttpsStatusCode.SUCCESS, flights);
    }

    /**
     * 항공편 목록 조회 (관리자)
     */
    @ReadTx
    public ApiEntity<PageRes<FlightDto.SearchRes>> getFlights(FlightDto.SearchReq req) {
        PageRes<FlightDto.SearchRes> flights = flightDao.findFlights(req);
        return ApiEntity.of(HttpsStatusCode.SUCCESS, flights);
    }

    /**
     * 항공편 상세 조회 (사용자, 운항 중 항공편만)
     */
    @ReadTx
    public ApiEntity<FlightDto.DetailRes> getFlightDetail(Long flightId) {
        FlightDto.DetailRes flight = flightDao.findFlightDetail(flightId);
        if (flight == null || !Flight.STATUS_ACTIVE.equals(flight.getStatus())) {
            throw business(HttpsStatusCode.RESOURCE_NOT_FOUND);
        }
        return ApiEntity.of(HttpsStatusCode.SUCCESS, flight);
    }

    /**
     * 항공편 상세 (관리자, 재고 현황 포함)
     */
    @ReadTx
    public ApiEntity<FlightDto.AdminDetailRes> getAdminFlightDetail(Long flightId) {
        FlightDto.AdminDetailRes detail = flightDao.findAdminFlightDetail(flightId);
        if (detail == null) throw business(HttpsStatusCode.RESOURCE_NOT_FOUND);

        LocalDate today = LocalDate.now();
        detail.setUpcomingInventories(flightDao.findUpcomingInventories(flightId, today, UPCOMING_LIMIT));
        detail.setUpcomingInventoryCount(flightDao.countUpcomingInventories(flightId, today));
        detail.setLastInventoryDate(flightDao.findLastInventoryDate(flightId));
        return ApiEntity.of(HttpsStatusCode.SUCCESS, detail);
    }

    /**
     * 항공편 등록 (소요 시간은 출발·도착 시각으로 계산)
     */
    @RequiredTx
    public ApiEntity<Void> createFlight(FlightDto.CreateReq req) {
        String flightNumber = req.getFlightNumber() == null ? "" : req.getFlightNumber().trim().toUpperCase();
        if (!FLIGHT_NUMBER.matcher(flightNumber).matches()) {
            throw business("편명 형식이 올바르지 않습니다. (예: KE1234)", HttpsStatusCode.INVALID_PARAMETER);
        }
        if (flightRepository.existsByFlightNumber(flightNumber)) {
            throw business("이미 등록된 편명입니다.", HttpsStatusCode.DUPLICATE_ITEM);
        }
        validate(req.getAirlineCode(), req.getDepartureAirport(), req.getArrivalAirport(),
                req.getDepartureTime(), req.getArrivalTime(), req.getBasePrice(), req.getTotalSeats());
        if (!flightNumber.startsWith(req.getAirlineCode())) {
            throw business("편명은 항공사 코드(" + req.getAirlineCode() + ")로 시작해야 합니다.", HttpsStatusCode.INVALID_PARAMETER);
        }
        String status = req.getStatus() != null ? req.getStatus() : Flight.STATUS_ACTIVE;
        if (!STATUSES.contains(status)) {
            throw business("상태가 올바르지 않습니다.", HttpsStatusCode.INVALID_PARAMETER);
        }

        Flight flight = Flight.builder()
                .flightNumber(flightNumber)
                .airlineCode(req.getAirlineCode())
                .departureAirport(req.getDepartureAirport())
                .arrivalAirport(req.getArrivalAirport())
                .departureTime(req.getDepartureTime())
                .arrivalTime(req.getArrivalTime())
                .durationMinutes(durationMinutes(req.getDepartureTime(), req.getArrivalTime()))
                .totalSeats(req.getTotalSeats())
                .basePrice(req.getBasePrice())
                .status(status)
                .build();
        flightRepository.save(flight);
        return ApiEntity.of(HttpsStatusCode.REGISTER);
    }

    /**
     * 항공편 수정 (편명 변경 불가, 재고가 있으면 좌석 수 변경 불가)
     */
    @RequiredTx
    public ApiEntity<Void> updateFlight(Long flightId, FlightDto.UpdateReq req) {
        Flight flight = flightRepository.findById(flightId)
                .orElseThrow(() -> business(HttpsStatusCode.RESOURCE_NOT_FOUND));

        validate(req.getAirlineCode(), req.getDepartureAirport(), req.getArrivalAirport(),
                req.getDepartureTime(), req.getArrivalTime(), req.getBasePrice(), req.getTotalSeats());
        if (!flight.getFlightNumber().startsWith(req.getAirlineCode())) {
            throw business("편명과 항공사 코드가 일치하지 않습니다.", HttpsStatusCode.INVALID_PARAMETER);
        }
        if (req.getStatus() == null || !STATUSES.contains(req.getStatus())) {
            throw business("상태가 올바르지 않습니다.", HttpsStatusCode.INVALID_PARAMETER);
        }
        if (!flight.getTotalSeats().equals(req.getTotalSeats()) && flightInventoryRepository.existsByFlightId(flightId)) {
            throw business("재고가 생성된 항공편은 좌석 수를 변경할 수 없습니다.", HttpsStatusCode.UNPROCESSABLE_ENTITY);
        }

        req.setDurationMinutes(durationMinutes(req.getDepartureTime(), req.getArrivalTime()));
        flight.updateFlight(req);
        return ApiEntity.of(HttpsStatusCode.SUCCESS);
    }

    /**
     * 항공편 운항 중지 (status = inactive, 데이터·기존 예약 유지)
     */
    @RequiredTx
    public ApiEntity<Void> deleteFlight(Long flightId) {
        Flight flight = flightRepository.findById(flightId)
                .orElseThrow(() -> business(HttpsStatusCode.RESOURCE_NOT_FOUND));
        flight.deleteFlight();
        return ApiEntity.of(HttpsStatusCode.SUCCESS);
    }

    /**
     * 재고 상세 조회
     */
    @ReadTx
    public ApiEntity<FlightInventoryDto.DetailRes> getInventoryDetail(Long inventoryId) {
        FlightInventoryDto.DetailRes inventory = flightDao.findInventoryDetail(inventoryId);
        return ApiEntity.of(HttpsStatusCode.SUCCESS, inventory);
    }

    /**
     * 재고 수동 조정
     */
    @RequiredTx
    public ApiEntity<Void> adjustInventory(Long inventoryId, FlightInventoryDto.AdjustReq req) {
        flightInventoryService.adjust(inventoryId, req);
        return ApiEntity.of(HttpsStatusCode.SUCCESS);
    }

    /**
     * 재고 일괄 생성 (오늘 이후, 최대 90일, 이미 있는 날짜는 건너뜀)
     */
    @RequiredTx
    public ApiEntity<FlightInventoryDto.GenerateRes> generateInventory(Long flightId, FlightInventoryDto.GenerateReq req) {
        Flight flight = flightRepository.findById(flightId)
                .orElseThrow(() -> business(HttpsStatusCode.RESOURCE_NOT_FOUND));
        if (!Flight.STATUS_ACTIVE.equals(flight.getStatus())) {
            throw business("운항 중지된 항공편은 재고를 생성할 수 없습니다.", HttpsStatusCode.UNPROCESSABLE_ENTITY);
        }

        LocalDate start = req.getStartDate();
        LocalDate end = req.getEndDate();
        if (start == null || end == null) {
            throw business("생성 기간을 입력해주세요.", HttpsStatusCode.INVALID_PARAMETER);
        }
        if (start.isBefore(LocalDate.now())) {
            throw business("시작일은 오늘 이후여야 합니다.", HttpsStatusCode.INVALID_PARAMETER);
        }
        if (end.isBefore(start)) {
            throw business("종료일이 시작일보다 빠릅니다.", HttpsStatusCode.INVALID_PARAMETER);
        }
        if (ChronoUnit.DAYS.between(start, end) + 1 > MAX_GENERATE_DAYS) {
            throw business("한 번에 최대 " + MAX_GENERATE_DAYS + "일까지 생성할 수 있습니다.", HttpsStatusCode.INVALID_PARAMETER);
        }

        Set<LocalDate> existDates = new HashSet<>(flightDao.findInventoryDatesByFlightId(flightId));
        List<FlightInventory> inventories = start.datesUntil(end.plusDays(1))
                .filter(date -> !existDates.contains(date))
                .map(date -> FlightInventory.builder()
                        .flightId(flightId)
                        .flightDate(date)
                        .availableSeats(flight.getTotalSeats())
                        .build())
                .toList();

        flightInventoryRepository.saveAll(inventories);
        int total = (int) ChronoUnit.DAYS.between(start, end) + 1;
        return ApiEntity.of(HttpsStatusCode.REGISTER, FlightInventoryDto.GenerateRes.builder()
                .createdCount(inventories.size())
                .skippedCount(total - inventories.size())
                .build());
    }

    private void validate(String airlineCode, String departure, String arrival,
                          LocalTime departureTime, LocalTime arrivalTime, Integer basePrice, Integer totalSeats) {
        if (airlineCode == null || !airlineRepository.existsById(airlineCode)) {
            throw business("항공사를 선택해주세요.", HttpsStatusCode.INVALID_PARAMETER);
        }
        if (departure == null || !airportRepository.existsById(departure)
                || arrival == null || !airportRepository.existsById(arrival)) {
            throw business("출발지와 도착지를 선택해주세요.", HttpsStatusCode.INVALID_PARAMETER);
        }
        if (departure.equals(arrival)) {
            throw business("출발지와 도착지가 같습니다.", HttpsStatusCode.INVALID_PARAMETER);
        }
        if (departureTime == null || arrivalTime == null) {
            throw business("출발·도착 시간을 입력해주세요.", HttpsStatusCode.INVALID_PARAMETER);
        }
        if (departureTime.equals(arrivalTime)) {
            throw business("출발 시간과 도착 시간이 같습니다.", HttpsStatusCode.INVALID_PARAMETER);
        }
        if (basePrice == null || basePrice <= 0) {
            throw business("기본 운임을 입력해주세요.", HttpsStatusCode.INVALID_PARAMETER);
        }
        if (totalSeats == null || totalSeats < MIN_SEATS || totalSeats > MAX_SEATS) {
            throw business("총 좌석 수는 " + MIN_SEATS + "~" + MAX_SEATS + "석이어야 합니다.", HttpsStatusCode.INVALID_PARAMETER);
        }
    }

    /**
     * 소요 시간(분) — 도착이 출발보다 이르면 다음날 도착으로 계산
     */
    private int durationMinutes(LocalTime departure, LocalTime arrival) {
        long minutes = Duration.between(departure, arrival).toMinutes();
        return (int) (minutes > 0 ? minutes : minutes + 24 * 60);
    }

}
