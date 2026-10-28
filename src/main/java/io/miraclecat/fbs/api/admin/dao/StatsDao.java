package io.miraclecat.fbs.api.admin.dao;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.DateExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import io.miraclecat.fbs.api.admin.dto.StatsDto;
import io.miraclecat.fbs.api.booking.domain.entity.Booking;
import io.miraclecat.fbs.api.booking.domain.entity.QBooking;
import io.miraclecat.fbs.api.code.domain.entity.QAirline;
import io.miraclecat.fbs.api.code.domain.entity.QAirport;
import io.miraclecat.fbs.api.flight.domain.entity.Flight;
import io.miraclecat.fbs.api.flight.domain.entity.QFlight;
import io.miraclecat.fbs.api.flight.domain.entity.QFlightInventory;
import io.miraclecat.fbs.api.member.domain.entity.QMember;
import io.miraclecat.fbs.api.payment.domain.entity.QPayment;
import io.miraclecat.fbs.core.auth.AuthRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * packageName    : io.miraclecat.fbs.api.admin.dao
 * fileName       : StatsDao
 * author         : MiracleCat
 * date           : 2026-10-27
 * description    : 관리자 대시보드 / 통계 집계 쿼리 (기간은 [from, to) )
 */
@Repository
@RequiredArgsConstructor
public class StatsDao {

    private final JPAQueryFactory queryFactory;

    private final QBooking booking = QBooking.booking;
    private final QPayment payment = QPayment.payment;
    private final QMember member = QMember.member;
    private final QFlight flight = QFlight.flight;
    private final QFlightInventory flightInventory = QFlightInventory.flightInventory;
    private final QAirline airline = QAirline.airline;
    private final QAirport departureAirportQ = new QAirport("departureAirport");
    private final QAirport arrivalAirportQ = new QAirport("arrivalAirport");

    /**
     * 예약 건수 (예약 생성일 기준, status null이면 전체)
     */
    public long countBookings(LocalDateTime from, LocalDateTime to, String status) {
        Long count = queryFactory
                .select(booking.count())
                .from(booking)
                .where(booking.createdAt.goe(from), booking.createdAt.lt(to),
                        status != null ? booking.status.eq(status) : null)
                .fetchOne();
        return count != null ? count : 0L;
    }

    /**
     * 순매출 = 승인 금액 합 - 환불 금액 합 (결제 승인일 기준)
     */
    public long sumNetSales(LocalDateTime from, LocalDateTime to) {
        NumberExpression<Long> amount = payment.amount.sumLong();
        NumberExpression<Long> refund = payment.refundAmount.sumLong();
        Tuple t = queryFactory
                .select(amount, refund)
                .from(payment)
                .where(payment.approvedAt.goe(from), payment.approvedAt.lt(to))
                .fetchOne();
        if (t == null) return 0L;
        return nvl(t.get(amount)) - nvl(t.get(refund));
    }

    /**
     * 신규 회원 (일반 회원)
     */
    public long countNewMembers(LocalDateTime from, LocalDateTime to) {
        Long count = queryFactory
                .select(member.count())
                .from(member)
                .where(member.role.eq(AuthRole.ROLE_USER), member.createdAt.goe(from), member.createdAt.lt(to))
                .fetchOne();
        return count != null ? count : 0L;
    }

    /**
     * 전체 회원 (탈퇴 제외 일반 회원)
     */
    public long countMembers() {
        Long count = queryFactory
                .select(member.count())
                .from(member)
                .where(member.role.eq(AuthRole.ROLE_USER), member.withdrawnAt.isNull())
                .fetchOne();
        return count != null ? count : 0L;
    }

    public long countActiveFlights() {
        Long count = queryFactory
                .select(flight.count())
                .from(flight)
                .where(flight.status.eq(Flight.STATUS_ACTIVE))
                .fetchOne();
        return count != null ? count : 0L;
    }

    /**
     * 해당 날짜에 재고가 있는 운항 중 항공편 수
     */
    public long countFlightsOn(LocalDate date) {
        Long count = queryFactory
                .select(flightInventory.count())
                .from(flightInventory)
                .join(flight).on(flight.id.eq(flightInventory.flightId))
                .where(flightInventory.flightDate.eq(date), flight.status.eq(Flight.STATUS_ACTIVE))
                .fetchOne();
        return count != null ? count : 0L;
    }

    /**
     * 일별 예약 건수 (예약 생성일)
     */
    public List<StatsDto.DailyCount> dailyBookings(LocalDateTime from, LocalDateTime to) {
        DateExpression<java.sql.Date> day = Expressions.dateTemplate(java.sql.Date.class, "cast({0} as date)", booking.createdAt);
        return queryFactory
                .select(day, booking.count())
                .from(booking)
                .where(booking.createdAt.goe(from), booking.createdAt.lt(to))
                .groupBy(day)
                .orderBy(day.asc())
                .fetch()
                .stream()
                .map(t -> StatsDto.DailyCount.builder()
                        .date(t.get(day) != null ? t.get(day).toLocalDate() : null)
                        .count(nvl(t.get(booking.count())))
                        .build())
                .toList();
    }

    /**
     * 항공사별 확정 예약 건수
     */
    public List<StatsDto.NameCount> airlineShares(LocalDateTime from, LocalDateTime to) {
        NumberExpression<Long> cnt = booking.count();
        return queryFactory
                .select(airline.nameKo, cnt)
                .from(booking)
                .join(flight).on(flight.id.eq(booking.flightId))
                .leftJoin(airline).on(airline.code.eq(flight.airlineCode))
                .where(booking.createdAt.goe(from), booking.createdAt.lt(to),
                        booking.status.eq(Booking.STATUS_CONFIRMED))
                .groupBy(airline.nameKo)
                .orderBy(cnt.desc())
                .fetch()
                .stream()
                .map(t -> StatsDto.NameCount.builder()
                        .name(t.get(airline.nameKo) != null ? t.get(airline.nameKo) : "기타")
                        .count(nvl(t.get(cnt)))
                        .build())
                .toList();
    }

    /**
     * 노선별 확정 예약 건수·매출 (상위 limit)
     */
    public List<StatsDto.RouteStat> topRoutes(LocalDateTime from, LocalDateTime to, int limit) {
        NumberExpression<Long> cnt = booking.count();
        NumberExpression<Long> revenue = booking.totalAmount.sumLong();
        return queryFactory
                .select(flight.departureAirport, departureAirportQ.nameKo, flight.arrivalAirport, arrivalAirportQ.nameKo,
                        cnt, revenue)
                .from(booking)
                .join(flight).on(flight.id.eq(booking.flightId))
                .leftJoin(departureAirportQ).on(departureAirportQ.code.eq(flight.departureAirport))
                .leftJoin(arrivalAirportQ).on(arrivalAirportQ.code.eq(flight.arrivalAirport))
                .where(booking.createdAt.goe(from), booking.createdAt.lt(to),
                        booking.status.eq(Booking.STATUS_CONFIRMED))
                .groupBy(flight.departureAirport, departureAirportQ.nameKo, flight.arrivalAirport, arrivalAirportQ.nameKo)
                .orderBy(cnt.desc(), revenue.desc())
                .limit(limit)
                .fetch()
                .stream()
                .map(t -> StatsDto.RouteStat.builder()
                        .departureAirport(t.get(flight.departureAirport))
                        .departureAirportNameKo(t.get(departureAirportQ.nameKo))
                        .arrivalAirport(t.get(flight.arrivalAirport))
                        .arrivalAirportNameKo(t.get(arrivalAirportQ.nameKo))
                        .count(nvl(t.get(cnt)))
                        .revenue(nvl(t.get(revenue)))
                        .build())
                .toList();
    }

    /**
     * 월별 순매출 (결제 승인일 기준)
     */
    public List<StatsDto.MonthSales> monthlySales(LocalDateTime from, LocalDateTime to) {
        NumberExpression<Integer> year = payment.approvedAt.year();
        NumberExpression<Integer> month = payment.approvedAt.month();
        NumberExpression<Long> amount = payment.amount.sumLong();
        NumberExpression<Long> refund = payment.refundAmount.sumLong();
        return queryFactory
                .select(year, month, amount, refund)
                .from(payment)
                .where(payment.approvedAt.goe(from), payment.approvedAt.lt(to))
                .groupBy(year, month)
                .orderBy(year.asc(), month.asc())
                .fetch()
                .stream()
                .map(t -> StatsDto.MonthSales.builder()
                        .month(String.format("%d-%02d", t.get(year), t.get(month)))
                        .amount(nvl(t.get(amount)) - nvl(t.get(refund)))
                        .build())
                .toList();
    }

    private long nvl(Long value) {
        return value != null ? value : 0L;
    }
}
