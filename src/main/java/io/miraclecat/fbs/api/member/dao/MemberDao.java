/**
 * packageName    : io.miraclecat.fbs.api.member.dao
 * fileName       : MemberDao
 * author         : jiho
 * date           : 26. 9. 22.
 * description    : 회원 QueryDSL 조회
 */
package io.miraclecat.fbs.api.member.dao;

import com.querydsl.core.types.Expression;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import io.miraclecat.fbs.api.booking.domain.entity.Booking;
import io.miraclecat.fbs.api.booking.domain.entity.QBooking;
import io.miraclecat.fbs.api.code.domain.entity.QAirport;
import io.miraclecat.fbs.api.flight.domain.entity.QFlight;
import io.miraclecat.fbs.api.member.domain.entity.Member;
import io.miraclecat.fbs.api.member.domain.entity.QMember;
import io.miraclecat.fbs.api.member.dto.MemberAdminDto;
import io.miraclecat.fbs.core.auth.AuthRole;
import io.miraclecat.fbs.core.utils.persistence.PageRes;
import io.miraclecat.fbs.core.utils.persistence.QueryDslHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class MemberDao {

    private final JPAQueryFactory queryFactory;

    private final QMember member = QMember.member;
    private final QBooking booking = QBooking.booking;
    private final QFlight flight = QFlight.flight;
    private final QAirport departureAirportQ = new QAirport("departureAirport");
    private final QAirport arrivalAirportQ = new QAirport("arrivalAirport");

    /**
	 * 회원 유무 확인
	 */
	public boolean checkMemberId(String loginId) {
        return queryFactory
                .selectOne()
                .from(member)
                .where(member.loginId.eq(loginId))
                .fetchFirst() != null;
	}

    /**
     * 회원 목록 (관리자, 일반 회원만)
     */
    public PageRes<MemberAdminDto.SearchRes> findAdminMembers(MemberAdminDto.SearchReq req) {
        Predicate[] conditions = adminConditions(req);

        List<MemberAdminDto.SearchRes> content = queryFactory
                .select(Projections.bean(MemberAdminDto.SearchRes.class,
                        member.memberId,
                        member.loginId,
                        member.memberName,
                        member.mobile,
                        member.provider,
                        member.status,
                        member.withdrawnAt,
                        member.createdAt,
                        bookingCount(Booking.STATUS_CONFIRMED, "bookingCount")
                ))
                .from(member)
                .where(conditions)
                .orderBy(member.createdAt.desc(), member.memberId.desc())
                .offset(req.getOffset())
                .limit(req.getSize())
                .fetch();

        Long totalCount = queryFactory
                .select(member.count())
                .from(member)
                .where(conditions)
                .fetchOne();

        return PageRes.of(content, totalCount != null ? totalCount : 0L, req);
    }

    /**
     * 탈퇴는 withdrawn_at 기준, 활성/정지는 탈퇴하지 않은 회원 중 status 기준
     */
    private Predicate[] adminConditions(MemberAdminDto.SearchReq req) {
        BooleanExpression status = switch (req.getStatus() == null ? "" : req.getStatus()) {
            case Member.STATUS_WITHDRAWN -> member.withdrawnAt.isNotNull();
            case Member.STATUS_ACTIVE -> member.withdrawnAt.isNull().and(member.status.eq(Member.STATUS_ACTIVE));
            case Member.STATUS_SUSPENDED -> member.withdrawnAt.isNull().and(member.status.eq(Member.STATUS_SUSPENDED));
            default -> null;
        };
        BooleanExpression joinDate = req.getJoinDate() == null ? null
                : member.createdAt.goe(req.getJoinDate().atStartOfDay())
                    .and(member.createdAt.lt(req.getJoinDate().plusDays(1).atStartOfDay()));

        return new Predicate[]{
                member.role.eq(AuthRole.ROLE_USER),
                QueryDslHelper.contains(member.loginId, req.getLoginId()),
                QueryDslHelper.contains(member.memberName, req.getMemberName()),
                QueryDslHelper.contains(member.mobile, req.getMobile()),
                status, joinDate
        };
    }

    /**
     * 회원 상세 (관리자)
     */
    public MemberAdminDto.DetailRes findAdminMemberDetail(Long memberId) {
        return queryFactory
                .select(Projections.bean(MemberAdminDto.DetailRes.class,
                        member.memberId,
                        member.loginId,
                        member.memberName,
                        member.mobile,
                        member.provider,
                        member.status,
                        member.withdrawnAt,
                        member.createdAt,
                        bookingCount(Booking.STATUS_CONFIRMED, "bookingCount"),
                        bookingCount(Booking.STATUS_CANCELLED, "cancelledCount")
                ))
                .from(member)
                .where(member.memberId.eq(memberId), member.role.eq(AuthRole.ROLE_USER))
                .fetchOne();
    }

    /**
     * 회원별 예약 건수 서브쿼리
     */
    private Expression<Long> bookingCount(String status, String alias) {
        return ExpressionUtils.as(
                JPAExpressions.select(booking.count())
                        .from(booking)
                        .where(booking.memberId.eq(member.memberId), booking.status.eq(status)),
                alias);
    }

    /**
     * 최근 예약 (관리자 회원 상세)
     */
    public List<MemberAdminDto.RecentBooking> findRecentBookings(Long memberId, int limit) {
        return queryFactory
                .select(Projections.bean(MemberAdminDto.RecentBooking.class,
                        booking.id,
                        booking.bookingNumber,
                        flight.flightNumber,
                        departureAirportQ.nameKo.as("departureAirportNameKo"),
                        arrivalAirportQ.nameKo.as("arrivalAirportNameKo"),
                        booking.flightDate,
                        booking.passengerCount,
                        booking.totalAmount,
                        booking.status
                ))
                .from(booking)
                .leftJoin(flight).on(flight.id.eq(booking.flightId))
                .leftJoin(departureAirportQ).on(departureAirportQ.code.eq(flight.departureAirport))
                .leftJoin(arrivalAirportQ).on(arrivalAirportQ.code.eq(flight.arrivalAirport))
                .where(booking.memberId.eq(memberId))
                .orderBy(booking.createdAt.desc(), booking.id.desc())
                .limit(limit)
                .fetch();
    }

}
