/**
 * packageName    : io.miraclecat.fbs.api.notice.dao
 * fileName       : NoticeDao
 * author         : MiracleCat
 * date           : 2026-05-19
 * description    : 공지사항 QueryDSL 조회 (작성자명: member 조인)
 */
package io.miraclecat.fbs.api.notice.dao;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import io.miraclecat.fbs.api.member.domain.entity.QMember;
import io.miraclecat.fbs.api.notice.domain.entity.QNotice;
import io.miraclecat.fbs.api.notice.dto.NoticeDto;
import io.miraclecat.fbs.core.utils.persistence.QueryDslHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class NoticeDao {

    private final JPAQueryFactory queryFactory;

    private final QNotice notice = QNotice.notice;
    private final QMember member = QMember.member;

    /**
     * 공지사항 목록 조회 (중요 공지 우선, 최신순)
     */
    public List<NoticeDto.SearchRes> findNotices(NoticeDto.SearchReq req) {
        return queryFactory
                .select(Projections.bean(NoticeDto.SearchRes.class,
                        notice.id,
                        notice.title,
                        notice.isImportant,
                        notice.views,
                        notice.status,
                        member.memberName.as("authorName"),
                        notice.createdAt
                ))
                .from(notice)
                .leftJoin(member).on(member.memberId.eq(notice.createdId))
                .where(searchConditions(req))
                .orderBy(notice.isImportant.desc(), notice.createdAt.desc())
                .offset(req.getOffset())
                .limit(req.getSize())
                .fetch();
    }

    /**
     * 공지사항 목록 건수
     */
    public long countNotices(NoticeDto.SearchReq req) {
        Long count = queryFactory
                .select(notice.count())
                .from(notice)
                .leftJoin(member).on(member.memberId.eq(notice.createdId))
                .where(searchConditions(req))
                .fetchOne();
        return count != null ? count : 0L;
    }

    /**
     * 공지사항 상세 조회
     */
    public NoticeDto.DetailRes findNoticeDetail(Long noticeId) {
        return queryFactory
                .select(Projections.bean(NoticeDto.DetailRes.class,
                        notice.id,
                        notice.title,
                        notice.content,
                        notice.isImportant,
                        notice.views,
                        notice.status,
                        member.memberName.as("authorName"),
                        notice.createdAt,
                        notice.updatedAt
                ))
                .from(notice)
                .leftJoin(member).on(member.memberId.eq(notice.createdId))
                .where(notice.id.eq(noticeId))
                .fetchOne();
    }

    private BooleanExpression[] searchConditions(NoticeDto.SearchReq req) {
        return new BooleanExpression[]{
                QueryDslHelper.contains(notice.title, req.getTitle()),
                QueryDslHelper.contains(member.memberName, req.getAuthorName()),
                QueryDslHelper.eq(notice.isImportant, req.getIsImportant()),
                QueryDslHelper.eq(notice.status, req.getStatus()),
                req.getStartDate() != null ? notice.createdAt.goe(req.getStartDate().atStartOfDay()) : null,
                req.getEndDate() != null ? notice.createdAt.lt(req.getEndDate().plusDays(1).atStartOfDay()) : null
        };
    }

}
