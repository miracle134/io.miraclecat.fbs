/**
 * packageName    : io.miraclecat.fbs.api.notice.controller
 * fileName       : NoticeRestController
 * author         : MiracleCat
 * date           : 2026-05-19
 * description    : 공지사항 컨트롤러
 *                  - /api/notice       : 사용자 (게시 공지 조회)
 *                  - /api/admin/notice : 관리자 (SecurityConfig에서 관리자 권한 검사)
 */
package io.miraclecat.fbs.api.notice.controller;

import io.miraclecat.fbs.api.notice.dto.NoticeDto;
import io.miraclecat.fbs.api.notice.service.NoticeService;
import io.miraclecat.fbs.core.response.ApiEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
public class NoticeRestController {

    private final NoticeService noticeService;

    /**
     * 공지사항 목록 (사용자)
     */
    @GetMapping("/api/notice")
    public ApiEntity<?> getNotices(@ModelAttribute NoticeDto.SearchReq req) {
        return noticeService.getNotices(req);
    }

    /**
     * 공지사항 상세 (사용자)
     */
    @GetMapping("/api/notice/{notice-id}")
    public ApiEntity<?> getNoticeDetail(@PathVariable("notice-id") Long noticeId) {
        return noticeService.getNoticeDetail(noticeId);
    }

    /**
     * 공지사항 목록 (관리자)
     */
    @GetMapping("/api/admin/notice")
    public ApiEntity<?> getAdminNotices(@ModelAttribute NoticeDto.SearchReq req) {
        return noticeService.getAdminNotices(req);
    }

    /**
     * 공지사항 상세 (관리자)
     */
    @GetMapping("/api/admin/notice/{notice-id}")
    public ApiEntity<?> getAdminNoticeDetail(@PathVariable("notice-id") Long noticeId) {
        return noticeService.getAdminNoticeDetail(noticeId);
    }

    /**
     * 공지사항 등록 (관리자)
     */
    @PostMapping("/api/admin/notice")
    public ApiEntity<?> createNotice(@RequestBody NoticeDto.CreateReq req) {
        return noticeService.createNotice(req);
    }

    /**
     * 공지사항 수정 (관리자)
     */
    @PutMapping("/api/admin/notice/{notice-id}")
    public ApiEntity<?> updateNotice(@PathVariable("notice-id") Long noticeId,
                                     @RequestBody NoticeDto.UpdateReq req) {
        return noticeService.updateNotice(noticeId, req);
    }

    /**
     * 공지사항 삭제 (관리자, 숨김 처리)
     */
    @DeleteMapping("/api/admin/notice/{notice-id}")
    public ApiEntity<?> deleteNotice(@PathVariable("notice-id") Long noticeId) {
        return noticeService.deleteNotice(noticeId);
    }

}
