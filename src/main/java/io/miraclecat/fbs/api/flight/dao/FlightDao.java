/**
 * packageName    : io.miraclecat.fbs.api.flight.dao
 * fileName       : FlightDao
 * author         : MiracleCat
 * date           : 2026-05-19
 * description    : 항공편 도메인 QueryDSL 조회
 */
package io.miraclecat.fbs.api.flight.dao;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import io.miraclecat.fbs.api.code.domain.entity.QAirline;
import io.miraclecat.fbs.api.code.domain.entity.QAirport;
import io.miraclecat.fbs.api.flight.domain.entity.Flight;
import io.miraclecat.fbs.api.flight.domain.entity.QFlight;
import io.miraclecat.fbs.api.flight.domain.entity.QFlightInventory;
import io.miraclecat.fbs.api.flight.domain.entity.QSeatReservation;
import io.miraclecat.fbs.api.flight.dto.FlightDto;
import io.miraclecat.fbs.api.flight.dto.FlightInventoryDto;
import io.miraclecat.fbs.core.utils.persistence.PageRes;
import io.miraclecat.fbs.core.utils.persistence.QueryDslHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class FlightDao {

    private final JPAQueryFactory queryFactory;

    private final QFlight flight = QFlight.flight;
    private final QFlightInventory flightInventory = QFlightInventory.flightInventory;
    private final QAirline airline = QAirline.airline;
    private final QAirport departureAirportQ = new QAirport("departureAirport");
    private final QAirport arrivalAirportQ = new QAirport("arrivalAirport");
    private static final LocalTime NOON = LocalTime.NOON;
    private static final LocalTime EVENING_START = LocalTime.of(18, 0);

    /**
     * 항공편 검색 (날짜별 재고 기준)
     */
    public PageRes<FlightDto.SearchRes> searchFlights(FlightDto.SearchReq req) {
        List<FlightDto.SearchRes> content = queryFactory
                .select(Projections.bean(FlightDto.SearchRes.class,
                        flight.id,
                        flight.flightNumber,
                        flight.airlineCode,
                        airline.nameKo.as("airlineNameKo"),
                        airline.logoUrl,
                        flight.departureAirport,
                        departureAirportQ.nameKo.as("departureAirportNameKo"),
                        flight.arrivalAirport,
                        arrivalAirportQ.nameKo.as("arrivalAirportNameKo"),
                        flight.departureTime,
                        flight.arrivalTime,
                        flight.durationMinutes,
                        flight.totalSeats,
                        flight.basePrice,
                        flight.status,
                        flightInventory.id.as("flightInventoryId"),
                        flightInventory.flightDate,
                        flightInventory.availableSeats
                ))
                .from(flight)
                .join(flightInventory).on(
                        flightInventory.flightId.eq(flight.id),
                        flightInventory.flightDate.eq(req.getFlightDate()))
                .leftJoin(airline).on(airline.code.eq(flight.airlineCode))
                .leftJoin(departureAirportQ).on(departureAirportQ.code.eq(flight.departureAirport))
                .leftJoin(arrivalAirportQ).on(arrivalAirportQ.code.eq(flight.arrivalAirport))
                .where(searchConditions(req))
                .orderBy(searchOrders(req))
                .offset(req.getOffset())
                .limit(req.getSize())
                .fetch();

        Long totalCount = queryFactory
                .select(flight.count())
                .from(flight)
                .join(flightInventory).on(
                        flightInventory.flightId.eq(flight.id),
                        flightInventory.flightDate.eq(req.getFlightDate()))
                .where(searchConditions(req))
                .fetchOne();

        return PageRes.of(content, totalCount != null ? totalCount : 0L, req);
    }

    /** content/count 공통 where 조건 (사용자 검색은 운항 중 항공편만) */
    private Predicate[] searchConditions(FlightDto.SearchReq req) {
        return new Predicate[] {
                flight.status.eq(Flight.STATUS_ACTIVE),
                QueryDslHelper.contains(flight.flightNumber, req.getFlightNumber()),
                QueryDslHelper.in(flight.airlineCode, req.getAirlineCodes()),
                QueryDslHelper.eq(flight.departureAirport, req.getDepartureAirport()),
                QueryDslHelper.eq(flight.arrivalAirport, req.getArrivalAirport()),
                QueryDslHelper.goe(flight.basePrice, req.getMinPrice()),
                QueryDslHelper.loe(flight.basePrice, req.getMaxPrice()),
                QueryDslHelper.goe(flightInventory.availableSeats, req.getPassengers()),
                departureTimeSlot(req.getDepartureTimeSlots())
        };
    }

    /** 출발 시간대: 오전 00~12 / 오후 12~18 / 저녁 18~24 (다중 선택은 OR) */
    private Predicate departureTimeSlot(List<String> slots) {
        if (slots == null || slots.isEmpty()) return null;

        BooleanBuilder builder = new BooleanBuilder();
        for (String slot : slots) {
            if ("MORNING".equals(slot)) {
                builder.or(flight.departureTime.lt(NOON));
            } else if ("AFTERNOON".equals(slot)) {
                builder.or(flight.departureTime.goe(NOON).and(flight.departureTime.lt(EVENING_START)));
            } else if ("EVENING".equals(slot)) {
                builder.or(flight.departureTime.goe(EVENING_START));
            }
        }
        return builder.hasValue() ? builder : null;
    }

    /** 정렬 조건 (마지막에 flightNumber를 붙여 페이징 순서 고정) */
    private OrderSpecifier<?>[] searchOrders(FlightDto.SearchReq req) {
        String sortType = req.getSortType();

        if ("PRICE_ASC".equals(sortType)) {
            return new OrderSpecifier<?>[]{
                    flight.basePrice.asc(), flight.departureTime.asc(), flight.flightNumber.asc()};
        }
        if ("DEPARTURE_ASC".equals(sortType)) {
            return new OrderSpecifier<?>[]{
                    flight.departureTime.asc(), flight.flightNumber.asc()};
        }
        if ("DURATION_ASC".equals(sortType)) {
            return new OrderSpecifier<?>[]{
                    flight.durationMinutes.asc(), flight.departureTime.asc(), flight.flightNumber.asc()};
        }
        return new OrderSpecifier<?>[]{flight.flightNumber.asc()}; // 기본값 (기존 동작 유지)
    }

    /**
     * 항공편 목록 조회
     */
    public PageRes<FlightDto.SearchRes> findFlights(FlightDto.SearchReq req) {
        List<FlightDto.SearchRes> content = queryFactory
                .select(Projections.bean(FlightDto.SearchRes.class,
                        flight.id,
                        flight.flightNumber,
                        flight.airlineCode,
                        airline.nameKo.as("airlineNameKo"),
                        airline.logoUrl,
                        flight.departureAirport,
                        departureAirportQ.nameKo.as("departureAirportNameKo"),
                        flight.arrivalAirport,
                        arrivalAirportQ.nameKo.as("arrivalAirportNameKo"),
                        flight.departureTime,
                        flight.arrivalTime,
                        flight.durationMinutes,
                        flight.totalSeats,
                        flight.basePrice,
                        flight.status
                ))
                .from(flight)
                .leftJoin(airline).on(airline.code.eq(flight.airlineCode))
                .leftJoin(departureAirportQ).on(departureAirportQ.code.eq(flight.departureAirport))
                .leftJoin(arrivalAirportQ).on(arrivalAirportQ.code.eq(flight.arrivalAirport))
                .where(
                        QueryDslHelper.contains(flight.flightNumber, req.getFlightNumber()),
                        QueryDslHelper.eq(flight.airlineCode, req.getAirlineCode()),
                        QueryDslHelper.eq(flight.departureAirport, req.getDepartureAirport()),
                        QueryDslHelper.eq(flight.arrivalAirport, req.getArrivalAirport()),
                        QueryDslHelper.eq(flight.status, req.getStatus())
                )
                .orderBy(flight.flightNumber.asc())
                .offset(req.getOffset())
                .limit(req.getSize())
                .fetch();

        Long totalCount = queryFactory
                .select(flight.count())
                .from(flight)
                .where(
                        QueryDslHelper.contains(flight.flightNumber, req.getFlightNumber()),
                        QueryDslHelper.eq(flight.airlineCode, req.getAirlineCode()),
                        QueryDslHelper.eq(flight.departureAirport, req.getDepartureAirport()),
                        QueryDslHelper.eq(flight.arrivalAirport, req.getArrivalAirport()),
                        QueryDslHelper.eq(flight.status, req.getStatus())
                )
                .fetchOne();

        return PageRes.of(content, totalCount != null ? totalCount : 0L, req);
    }

    /**
     * 항공편 상세 조회
     */
    public FlightDto.DetailRes findFlightDetail(Long flightId) {
        return queryFactory
                .select(Projections.bean(FlightDto.DetailRes.class,
                        flight.id,
                        flight.flightNumber,
                        flight.airlineCode,
                        airline.nameKo.as("airlineNameKo"),
                        airline.logoUrl,
                        flight.departureAirport,
                        departureAirportQ.nameKo.as("departureAirportNameKo"),
                        flight.arrivalAirport,
                        arrivalAirportQ.nameKo.as("arrivalAirportNameKo"),
                        flight.departureTime,
                        flight.arrivalTime,
                        flight.durationMinutes,
                        flight.totalSeats,
                        flight.basePrice,
                        flight.status
                ))
                .from(flight)
                .leftJoin(airline).on(airline.code.eq(flight.airlineCode))
                .leftJoin(departureAirportQ).on(departureAirportQ.code.eq(flight.departureAirport))
                .leftJoin(arrivalAirportQ).on(arrivalAirportQ.code.eq(flight.arrivalAirport))
                .where(flight.id.eq(flightId))
                .fetchOne();
    }

    /**
     * 재고 상세 조회
     */
    public FlightInventoryDto.DetailRes findInventoryDetail(Long inventoryId) {
        return queryFactory
                .select(Projections.bean(FlightInventoryDto.DetailRes.class,
                        flightInventory.id,
                        flightInventory.flightId,
                        flight.flightNumber,
                        flightInventory.flightDate,
                        flightInventory.availableSeats,
                        flightInventory.version
                ))
                .from(flightInventory)
                .leftJoin(flight).on(flight.id.eq(flightInventory.flightId))
                .where(flightInventory.id.eq(inventoryId))
                .fetchOne();
    }

    /**
     * 항공편 상세 (관리자)
     */
    public FlightDto.AdminDetailRes findAdminFlightDetail(Long flightId) {
        return queryFactory
                .select(Projections.bean(FlightDto.AdminDetailRes.class,
                        flight.id,
                        flight.flightNumber,
                        flight.airlineCode,
                        airline.nameKo.as("airlineNameKo"),
                        airline.logoUrl,
                        flight.departureAirport,
                        departureAirportQ.nameKo.as("departureAirportNameKo"),
                        flight.arrivalAirport,
                        arrivalAirportQ.nameKo.as("arrivalAirportNameKo"),
                        flight.departureTime,
                        flight.arrivalTime,
                        flight.durationMinutes,
                        flight.totalSeats,
                        flight.basePrice,
                        flight.status,
                        flight.createdAt,
                        flight.updatedAt
                ))
                .from(flight)
                .leftJoin(airline).on(airline.code.eq(flight.airlineCode))
                .leftJoin(departureAirportQ).on(departureAirportQ.code.eq(flight.departureAirport))
                .leftJoin(arrivalAirportQ).on(arrivalAirportQ.code.eq(flight.arrivalAirport))
                .where(flight.id.eq(flightId))
                .fetchOne();
    }

    /**
     * 오늘 이후 재고 (관리자 상세, 날짜순 limit건, 확정 예약 좌석 수 포함)
     */
    public List<FlightInventoryDto.SearchRes> findUpcomingInventories(Long flightId, LocalDate from, int limit) {
        QSeatReservation seatReservation = QSeatReservation.seatReservation;
        return queryFactory
                .select(Projections.bean(FlightInventoryDto.SearchRes.class,
                        flightInventory.id,
                        flightInventory.flightId,
                        flight.flightNumber,
                        flightInventory.flightDate,
                        flightInventory.availableSeats,
                        flightInventory.version,
                        ExpressionUtils.as(JPAExpressions
                                .select(seatReservation.count())
                                .from(seatReservation)
                                .where(seatReservation.inventoryId.eq(flightInventory.id)), "bookedSeats")
                ))
                .from(flightInventory)
                .leftJoin(flight).on(flight.id.eq(flightInventory.flightId))
                .where(flightInventory.flightId.eq(flightId), flightInventory.flightDate.goe(from))
                .orderBy(flightInventory.flightDate.asc())
                .limit(limit)
                .fetch();
    }

    public long countUpcomingInventories(Long flightId, LocalDate from) {
        Long count = queryFactory
                .select(flightInventory.count())
                .from(flightInventory)
                .where(flightInventory.flightId.eq(flightId), flightInventory.flightDate.goe(from))
                .fetchOne();
        return count != null ? count : 0L;
    }

    public LocalDate findLastInventoryDate(Long flightId) {
        return queryFactory
                .select(flightInventory.flightDate.max())
                .from(flightInventory)
                .where(flightInventory.flightId.eq(flightId))
                .fetchOne();
    }

    /**
     * 항공편의 기존 재고 날짜 목록 조회 (중복 skip용)
     */
    public List<LocalDate> findInventoryDatesByFlightId(Long flightId) {
        return queryFactory
                .select(flightInventory.flightDate)
                .from(flightInventory)
                .where(flightInventory.flightId.eq(flightId))
                .fetch();
    }

}
