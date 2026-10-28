/**
 * packageName    : io.miraclecat.fbs.api.booking.dao
 * fileName       : BookingDao
 * author         : MiracleCat
 * date           : 2026-05-19
 * description    : 예약 도메인 QueryDSL 조회
 */
package io.miraclecat.fbs.api.booking.dao;

import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import io.miraclecat.fbs.api.booking.domain.entity.Booking;
import io.miraclecat.fbs.api.booking.domain.entity.QBooking;
import io.miraclecat.fbs.api.booking.domain.entity.QBookingHistory;
import io.miraclecat.fbs.api.booking.domain.entity.QBookingPassenger;
import io.miraclecat.fbs.api.booking.dto.BookingDto;
import io.miraclecat.fbs.api.booking.dto.BookingHistoryDto;
import io.miraclecat.fbs.api.code.domain.entity.QAirline;
import io.miraclecat.fbs.api.code.domain.entity.QAirport;
import io.miraclecat.fbs.api.flight.domain.entity.QFlight;
import io.miraclecat.fbs.api.flight.domain.entity.QFlightInventory;
import io.miraclecat.fbs.api.member.domain.entity.QMember;
import io.miraclecat.fbs.api.payment.domain.entity.QPayment;
import io.miraclecat.fbs.core.utils.persistence.PageRes;
import io.miraclecat.fbs.core.utils.persistence.QueryDslHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class BookingDao {

    private final JPAQueryFactory queryFactory;

    private final QBooking booking = QBooking.booking;
    private final QBookingPassenger bookingPassenger = QBookingPassenger.bookingPassenger;
    private final QBookingHistory bookingHistory = QBookingHistory.bookingHistory;
    private final QFlight flight = QFlight.flight;
    private final QFlightInventory flightInventory = QFlightInventory.flightInventory;
    private final QAirline airline = QAirline.airline;
    private final QAirport departureAirportQ = new QAirport("departureAirport");
    private final QAirport arrivalAirportQ = new QAirport("arrivalAirport");
    private final QMember member = QMember.member;
    private final QPayment payment = QPayment.payment;

    /**
     * 예약 목록 (관리자, 페이징)
     */
    public PageRes<BookingDto.AdminSearchRes> findAdminBookings(BookingDto.AdminSearchReq req) {
        Predicate[] conditions = adminConditions(req);

        List<BookingDto.AdminSearchRes> content = queryFactory
                .select(Projections.bean(BookingDto.AdminSearchRes.class,
                        booking.id,
                        booking.bookingNumber,
                        booking.memberId,
                        member.memberName,
                        member.loginId,
                        flight.flightNumber,
                        flight.departureAirport,
                        departureAirportQ.nameKo.as("departureAirportNameKo"),
                        flight.arrivalAirport,
                        arrivalAirportQ.nameKo.as("arrivalAirportNameKo"),
                        booking.flightDate,
                        booking.passengerCount,
                        booking.totalAmount,
                        booking.status,
                        booking.createdAt
                ))
                .from(booking)
                .leftJoin(member).on(member.memberId.eq(booking.memberId))
                .leftJoin(flight).on(flight.id.eq(booking.flightId))
                .leftJoin(departureAirportQ).on(departureAirportQ.code.eq(flight.departureAirport))
                .leftJoin(arrivalAirportQ).on(arrivalAirportQ.code.eq(flight.arrivalAirport))
                .where(conditions)
                .orderBy(booking.createdAt.desc(), booking.id.desc())
                .offset(req.getOffset())
                .limit(req.getSize())
                .fetch();

        Long totalCount = queryFactory
                .select(booking.count())
                .from(booking)
                .leftJoin(member).on(member.memberId.eq(booking.memberId))
                .where(conditions)
                .fetchOne();

        return PageRes.of(content, totalCount != null ? totalCount : 0L, req);
    }

    /**
     * 관리자 목록 조건 (예약자: 회원 이름 또는 아이디)
     */
    private Predicate[] adminConditions(BookingDto.AdminSearchReq req) {
        boolean byFlightDate = "flight".equals(req.getDateType());
        BooleanExpression start = null;
        BooleanExpression end = null;
        if (byFlightDate) {
            start = QueryDslHelper.goe(booking.flightDate, req.getStartDate());
            end = QueryDslHelper.loe(booking.flightDate, req.getEndDate());
        } else {
            if (req.getStartDate() != null) start = booking.createdAt.goe(req.getStartDate().atStartOfDay());
            if (req.getEndDate() != null) end = booking.createdAt.lt(req.getEndDate().plusDays(1).atStartOfDay());
        }

        BooleanExpression booker = null;
        if (req.getBookerName() != null && !req.getBookerName().isBlank()) {
            String keyword = req.getBookerName().trim();
            booker = member.memberName.contains(keyword).or(member.loginId.contains(keyword));
        }

        return new Predicate[]{
                QueryDslHelper.contains(booking.bookingNumber, req.getBookingNumber()),
                QueryDslHelper.eq(booking.status, req.getStatus()),
                booker, start, end
        };
    }

    /**
     * 예약 상세 (관리자)
     */
    public BookingDto.AdminDetailRes findAdminBookingDetail(Long bookingId) {
        return queryFactory
                .select(Projections.bean(BookingDto.AdminDetailRes.class,
                        booking.id,
                        booking.bookingNumber,
                        booking.status,
                        booking.flightDate,
                        booking.passengerCount,
                        booking.fareAmount,
                        booking.bookingFeeAmount,
                        booking.totalAmount,
                        booking.cancelledBy,
                        booking.cancelReason,
                        booking.cancelledAt,
                        booking.createdAt,
                        booking.memberId,
                        member.memberName,
                        member.loginId,
                        member.mobile.as("memberMobile"),
                        flight.flightNumber,
                        airline.nameKo.as("airlineNameKo"),
                        flight.departureAirport,
                        departureAirportQ.nameKo.as("departureAirportNameKo"),
                        flight.arrivalAirport,
                        arrivalAirportQ.nameKo.as("arrivalAirportNameKo"),
                        flight.departureTime,
                        flight.arrivalTime,
                        payment.method.as("paymentMethod"),
                        payment.status.as("paymentStatus"),
                        payment.amount.as("paymentAmount"),
                        payment.refundAmount,
                        payment.approvedAt,
                        payment.cancelledAt.as("paymentCancelledAt")
                ))
                .from(booking)
                .leftJoin(member).on(member.memberId.eq(booking.memberId))
                .leftJoin(flight).on(flight.id.eq(booking.flightId))
                .leftJoin(airline).on(airline.code.eq(flight.airlineCode))
                .leftJoin(departureAirportQ).on(departureAirportQ.code.eq(flight.departureAirport))
                .leftJoin(arrivalAirportQ).on(arrivalAirportQ.code.eq(flight.arrivalAirport))
                .leftJoin(payment).on(payment.bookingId.eq(booking.id))
                .where(booking.id.eq(bookingId))
                .fetchOne();
    }

    /**
     * 예약 상세 - 탑승객 (관리자)
     */
    public List<BookingDto.AdminPassenger> findAdminPassengers(Long bookingId) {
        return queryFactory
                .select(Projections.bean(BookingDto.AdminPassenger.class,
                        bookingPassenger.id,
                        bookingPassenger.name,
                        bookingPassenger.nameEn,
                        bookingPassenger.gender,
                        bookingPassenger.birthDate,
                        bookingPassenger.email,
                        bookingPassenger.mobile,
                        bookingPassenger.seatNumber
                ))
                .from(bookingPassenger)
                .where(bookingPassenger.bookingId.eq(bookingId))
                .orderBy(bookingPassenger.id.asc())
                .fetch();
    }

    /**
     * 내 예약 목록 (페이징)
     */
    public PageRes<BookingDto.MyRes> findMyBookings(BookingDto.MyReq req, Long memberId) {
        Predicate[] conditions = myConditions(req, memberId);

        List<BookingDto.MyRes> content = queryFactory
                .select(Projections.bean(BookingDto.MyRes.class,
                        booking.id,
                        booking.bookingNumber,
                        booking.status,
                        booking.flightDate,
                        booking.passengerCount,
                        booking.fareAmount,
                        booking.bookingFeeAmount,
                        booking.totalAmount,
                        booking.createdAt,
                        booking.cancelledAt,
                        booking.flightId,
                        flightInventory.id.as("inventoryId"),
                        flight.flightNumber,
                        airline.nameKo.as("airlineNameKo"),
                        flight.departureAirport,
                        departureAirportQ.nameKo.as("departureAirportNameKo"),
                        flight.arrivalAirport,
                        arrivalAirportQ.nameKo.as("arrivalAirportNameKo"),
                        flight.departureTime,
                        flight.arrivalTime,
                        flight.durationMinutes
                ))
                .from(booking)
                .join(flight).on(flight.id.eq(booking.flightId))
                .leftJoin(airline).on(airline.code.eq(flight.airlineCode))
                .leftJoin(departureAirportQ).on(departureAirportQ.code.eq(flight.departureAirport))
                .leftJoin(arrivalAirportQ).on(arrivalAirportQ.code.eq(flight.arrivalAirport))
                .leftJoin(flightInventory).on(
                        flightInventory.flightId.eq(booking.flightId),
                        flightInventory.flightDate.eq(booking.flightDate))
                .where(conditions)
                .orderBy(booking.createdAt.desc(), booking.id.desc())
                .offset(req.getOffset())
                .limit(req.getSize())
                .fetch();

        Long totalCount = queryFactory
                .select(booking.count())
                .from(booking)
                .where(conditions)
                .fetchOne();

        return PageRes.of(content, totalCount != null ? totalCount : 0L, req);
    }

    /**
     * 예정: 확정 + 출발일 오늘 이후 / 완료: 확정 + 출발일 지남 / 취소: 취소
     */
    private Predicate[] myConditions(BookingDto.MyReq req, Long memberId) {
        LocalDate today = LocalDate.now();
        BooleanExpression filter = switch (req.getFilter() == null ? "all" : req.getFilter()) {
            case "upcoming" -> booking.status.eq(Booking.STATUS_CONFIRMED).and(booking.flightDate.goe(today));
            case "completed" -> booking.status.eq(Booking.STATUS_CONFIRMED).and(booking.flightDate.lt(today));
            case "cancelled" -> booking.status.eq(Booking.STATUS_CANCELLED);
            default -> null;
        };
        return new Predicate[]{booking.memberId.eq(memberId), filter};
    }

    /**
     * 예약별 탑승객 (목록용)
     */
    public List<BookingDto.MyPassenger> findMyPassengers(List<Long> bookingIds) {
        if (bookingIds == null || bookingIds.isEmpty()) return List.of();
        return queryFactory
                .select(Projections.bean(BookingDto.MyPassenger.class,
                        bookingPassenger.id,
                        bookingPassenger.bookingId,
                        bookingPassenger.name,
                        bookingPassenger.seatNumber
                ))
                .from(bookingPassenger)
                .where(bookingPassenger.bookingId.in(bookingIds))
                .orderBy(bookingPassenger.bookingId.asc(), bookingPassenger.id.asc())
                .fetch();
    }

    /**
     * 예약 이력 목록 조회
     */
    public List<BookingHistoryDto.SearchRes> findBookingHistories(BookingHistoryDto.SearchReq req) {
        return queryFactory
                .select(Projections.bean(BookingHistoryDto.SearchRes.class,
                        bookingHistory.id,
                        bookingHistory.bookingId,
                        booking.bookingNumber,
                        bookingHistory.previousStatus,
                        bookingHistory.currentStatus,
                        bookingHistory.changedBy,
                        bookingHistory.reason,
                        bookingHistory.createdAt
                ))
                .from(bookingHistory)
                .leftJoin(booking).on(booking.id.eq(bookingHistory.bookingId))
                .where(
                        QueryDslHelper.eq(bookingHistory.bookingId, req.getBookingId()),
                        QueryDslHelper.eq(bookingHistory.changedBy, req.getChangedBy())
                )
                .orderBy(bookingHistory.createdAt.desc())
                .fetch();
    }

}
