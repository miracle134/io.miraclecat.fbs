package io.miraclecat.fbs.api.member.service;

import io.miraclecat.fbs.api.member.dao.MemberDao;
import io.miraclecat.fbs.api.member.domain.entity.Member;
import io.miraclecat.fbs.api.member.domain.repository.MemberRepository;
import io.miraclecat.fbs.api.member.dto.MemberAdminDto;
import io.miraclecat.fbs.core.auth.AuthRole;
import io.miraclecat.fbs.core.config.ReadTx;
import io.miraclecat.fbs.core.config.RequiredTx;
import io.miraclecat.fbs.core.response.ApiEntity;
import io.miraclecat.fbs.core.response.HttpsStatusCode;
import io.miraclecat.fbs.core.utils.persistence.PageRes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static io.miraclecat.fbs.core.exception.GlobalException.business;

/**
 * packageName    : io.miraclecat.fbs.api.member.service
 * fileName       : MemberAdminService
 * author         : MiracleCat
 * date           : 2026-10-27
 * description    : 관리자 회원 관리 (일반 회원 목록 / 상세 / 정지·해제)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemberAdminService {

    private static final int RECENT_BOOKINGS = 5;

    private final MemberDao memberDao;
    private final MemberRepository memberRepository;

    @ReadTx
    public ApiEntity<PageRes<MemberAdminDto.SearchRes>> getMembers(MemberAdminDto.SearchReq req) {
        PageRes<MemberAdminDto.SearchRes> page = memberDao.findAdminMembers(req);
        page.getContent().forEach(m -> m.setStatus(viewStatus(m.getStatus(), m.getWithdrawnAt() != null)));
        return ApiEntity.of(HttpsStatusCode.SUCCESS, page);
    }

    @ReadTx
    public ApiEntity<MemberAdminDto.DetailRes> getMemberDetail(Long memberId) {
        MemberAdminDto.DetailRes detail = memberDao.findAdminMemberDetail(memberId);
        if (detail == null) throw business(HttpsStatusCode.RESOURCE_NOT_FOUND);
        detail.setStatus(viewStatus(detail.getStatus(), detail.getWithdrawnAt() != null));
        detail.setRecentBookings(memberDao.findRecentBookings(memberId, RECENT_BOOKINGS));
        return ApiEntity.of(HttpsStatusCode.SUCCESS, detail);
    }

    /**
     * 정지 / 해제 (탈퇴 회원·관리자 계정은 불가)
     * 이미 발급된 토큰은 만료 전까지 유효 — 다음 로그인부터 차단
     */
    @RequiredTx
    public ApiEntity<Void> changeStatus(Long memberId, MemberAdminDto.StatusReq req) {
        String status = req != null ? req.getStatus() : null;
        if (!Member.STATUS_ACTIVE.equals(status) && !Member.STATUS_SUSPENDED.equals(status)) {
            throw business("상태가 올바르지 않습니다.", HttpsStatusCode.INVALID_PARAMETER);
        }

        Member member = memberRepository.findById(memberId)
                .filter(m -> m.getRole() == AuthRole.ROLE_USER)
                .orElseThrow(() -> business(HttpsStatusCode.RESOURCE_NOT_FOUND));
        if (member.getWithdrawnAt() != null) {
            throw business("탈퇴한 회원은 상태를 변경할 수 없습니다.", HttpsStatusCode.UNPROCESSABLE_ENTITY);
        }

        member.changeStatus(status);
        log.info("[회원 상태 변경] memberId={}, status={}", memberId, status);
        return ApiEntity.of(HttpsStatusCode.SUCCESS);
    }

    private String viewStatus(String status, boolean withdrawn) {
        if (withdrawn) return Member.STATUS_WITHDRAWN;
        return status != null ? status : Member.STATUS_ACTIVE;
    }
}
