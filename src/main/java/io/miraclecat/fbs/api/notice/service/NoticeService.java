/**
 * packageName    : io.miraclecat.fbs.api.notice.service
 * fileName       : NoticeService
 * author         : MiracleCat
 * date           : 2026-05-19
 * description    : 공지사항 서비스
 *                  - 사용자: 게시(published) 공지만 조회, 상세 조회 시 조회수 증가
 *                  - 관리자: 전체 상태 조회, 등록·수정·숨김
 */
package io.miraclecat.fbs.api.notice.service;

import io.miraclecat.fbs.api.notice.dao.NoticeDao;
import io.miraclecat.fbs.api.notice.domain.entity.Notice;
import io.miraclecat.fbs.api.notice.domain.repository.NoticeRepository;
import io.miraclecat.fbs.api.notice.dto.NoticeDto;
import io.miraclecat.fbs.core.config.ReadTx;
import io.miraclecat.fbs.core.config.RequiredTx;
import io.miraclecat.fbs.core.response.ApiEntity;
import io.miraclecat.fbs.core.response.HttpsStatusCode;
import io.miraclecat.fbs.core.utils.persistence.PageRes;
import io.miraclecat.fbs.core.utils.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

import static io.miraclecat.fbs.core.exception.GlobalException.business;

@Service
@RequiredArgsConstructor
public class NoticeService {

    private static final Set<String> STATUSES = Set.of(Notice.STATUS_PUBLISHED, Notice.STATUS_HIDDEN);
    private static final int TITLE_MAX = 200;

    private final NoticeRepository noticeRepository;
    private final NoticeDao noticeDao;

    /**
     * 공지사항 목록 (사용자 — 게시 공지만)
     */
    @ReadTx
    public ApiEntity<PageRes<NoticeDto.SearchRes>> getNotices(NoticeDto.SearchReq req) {
        req.setStatus(Notice.STATUS_PUBLISHED);
        req.setAuthorName(null);
        return ApiEntity.of(HttpsStatusCode.SUCCESS, search(req));
    }

    /**
     * 공지사항 상세 (사용자 — 게시 공지만, 조회수 증가)
     */
    @RequiredTx
    public ApiEntity<NoticeDto.DetailRes> getNoticeDetail(Long noticeId) {
        Notice notice = noticeRepository.findById(noticeId)
                .filter(Notice::isPublished)
                .orElseThrow(() -> business("존재하지 않거나 비공개된 공지사항입니다.", HttpsStatusCode.RESOURCE_NOT_FOUND));
        notice.increaseViews();
        return ApiEntity.of(HttpsStatusCode.SUCCESS, noticeDao.findNoticeDetail(noticeId));
    }

    /**
     * 공지사항 목록 (관리자 — 전체 상태)
     */
    @ReadTx
    public ApiEntity<PageRes<NoticeDto.SearchRes>> getAdminNotices(NoticeDto.SearchReq req) {
        return ApiEntity.of(HttpsStatusCode.SUCCESS, search(req));
    }

    /**
     * 공지사항 상세 (관리자 — 조회수 증가 없음)
     */
    @ReadTx
    public ApiEntity<NoticeDto.DetailRes> getAdminNoticeDetail(Long noticeId) {
        NoticeDto.DetailRes detail = noticeDao.findNoticeDetail(noticeId);
        if (detail == null) throw business(HttpsStatusCode.RESOURCE_NOT_FOUND);
        return ApiEntity.of(HttpsStatusCode.SUCCESS, detail);
    }

    /**
     * 공지사항 등록
     */
    @RequiredTx
    public ApiEntity<Void> createNotice(NoticeDto.CreateReq req) {
        validate(req.getTitle(), req.getContent(), req.getStatus());
        Notice notice = Notice.builder()
                .title(req.getTitle().trim())
                .content(req.getContent())
                .isImportant(Boolean.TRUE.equals(req.getIsImportant()))
                .status(req.getStatus())
                .createdId(currentMemberId())
                .build();
        noticeRepository.save(notice);
        return ApiEntity.of(HttpsStatusCode.REGISTER);
    }

    /**
     * 공지사항 수정
     */
    @RequiredTx
    public ApiEntity<Void> updateNotice(Long noticeId, NoticeDto.UpdateReq req) {
        validate(req.getTitle(), req.getContent(), req.getStatus());
        Notice notice = findNotice(noticeId);
        req.setTitle(req.getTitle().trim());
        req.setIsImportant(Boolean.TRUE.equals(req.getIsImportant()));
        notice.updateNotice(req, currentMemberId());
        return ApiEntity.of(HttpsStatusCode.SUCCESS);
    }

    /**
     * 공지사항 삭제 (status = hidden, 데이터는 유지)
     */
    @RequiredTx
    public ApiEntity<Void> deleteNotice(Long noticeId) {
        findNotice(noticeId).deleteNotice(currentMemberId());
        return ApiEntity.of(HttpsStatusCode.SUCCESS);
    }

    private PageRes<NoticeDto.SearchRes> search(NoticeDto.SearchReq req) {
        List<NoticeDto.SearchRes> content = noticeDao.findNotices(req);
        long total = noticeDao.countNotices(req);
        return PageRes.of(content, total, req);
    }

    private Notice findNotice(Long noticeId) {
        return noticeRepository.findById(noticeId)
                .orElseThrow(() -> business(HttpsStatusCode.RESOURCE_NOT_FOUND));
    }

    private void validate(String title, String content, String status) {
        if (title == null || title.isBlank()) {
            throw business("제목을 입력해주세요.", HttpsStatusCode.INVALID_PARAMETER);
        }
        if (title.trim().length() > TITLE_MAX) {
            throw business("제목은 " + TITLE_MAX + "자 이하로 입력해주세요.", HttpsStatusCode.INVALID_PARAMETER);
        }
        if (content == null || content.isBlank()) {
            throw business("내용을 입력해주세요.", HttpsStatusCode.INVALID_PARAMETER);
        }
        if (status == null || !STATUSES.contains(status)) {
            throw business("상태가 올바르지 않습니다.", HttpsStatusCode.INVALID_PARAMETER);
        }
    }

    private Long currentMemberId() {
        Long memberId = SecurityUtil.getCurrentMemberId();
        if (memberId == null) throw business(HttpsStatusCode.LOGIN_REQUIRED);
        return memberId;
    }

}
