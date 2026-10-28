package io.miraclecat.fbs.api.member.service;

import io.miraclecat.fbs.api.member.dao.MemberDao;
import io.miraclecat.fbs.api.member.domain.entity.Member;
import io.miraclecat.fbs.api.member.domain.repository.MemberRepository;
import io.miraclecat.fbs.api.member.dto.MemberAccountDto;
import io.miraclecat.fbs.core.auth.AuthRole;
import io.miraclecat.fbs.core.auth.config.CustomAuthorityUtil;
import io.miraclecat.fbs.core.auth.dto.MemberLoginDto;
import io.miraclecat.fbs.core.auth.service.JwtService;
import io.miraclecat.fbs.core.config.ReadTx;
import io.miraclecat.fbs.core.config.RequiredTx;
import io.miraclecat.fbs.core.response.ApiEntity;
import io.miraclecat.fbs.core.response.HttpsStatusCode;
import io.miraclecat.fbs.core.utils.security.PasswordPolicy;
import io.miraclecat.fbs.core.utils.security.SecurityUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

import static io.miraclecat.fbs.core.exception.GlobalException.business;

/**
 * packageName    : io.miraclecat.fbs.api.member.service
 * fileName       : MemberAccountService
 * author         : MiracleCat
 * date           : 2026-10-27
 * description    : 회원가입 / 내 정보 조회 / 수정 / 비밀번호 변경 (로그인 회원 본인)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemberAccountService {

    private static final Pattern MOBILE = Pattern.compile("^[0-9+\\-()]{9,20}$");
    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$");

    private final MemberRepository memberRepository;
    private final MemberDao memberDao;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CustomAuthorityUtil customAuthorityUtil;

    /**
     * 회원가입 (일반 회원, 아이디 = 이메일)
     */
    @RequiredTx
    public ApiEntity<?> signup(MemberAccountDto.SignupReq req) {
        String loginId = req.getLoginId() == null ? "" : req.getLoginId().trim();
        if (loginId.isEmpty() || loginId.length() > 64 || !EMAIL.matcher(loginId).matches()) {
            throw business("이메일 형식이 올바르지 않습니다.", HttpsStatusCode.INVALID_PARAMETER);
        }
        if (!PasswordPolicy.isValid(req.getLoginPassword())) {
            throw business(PasswordPolicy.MESSAGE, HttpsStatusCode.INVALID_PARAMETER);
        }
        String name = validName(req.getMemberName());
        String mobile = validMobile(req.getMobile());
        if (memberDao.checkMemberId(loginId)) {
            throw business("이미 가입된 이메일입니다.", HttpsStatusCode.DUPLICATE_ITEM);
        }

        Member member = Member.createMember()
                .loginId(loginId)
                .loginPassword(passwordEncoder.encode(req.getLoginPassword()))
                .role(AuthRole.ROLE_USER)
                .memberName(name)
                .mobile(mobile)
                .build();
        memberRepository.save(member);
        return ApiEntity.ofMessage(HttpsStatusCode.REGISTER, "회원가입이 완료되었습니다.");
    }

    /**
     * 내 정보
     */
    @ReadTx
    public ApiEntity<MemberAccountDto.MeRes> getMe() {
        return ApiEntity.of(HttpsStatusCode.SUCCESS, toRes(currentMember()));
    }

    /**
     * 이름 / 전화번호 수정 → JWT 재발급 (토큰에 이름 포함)
     */
    @RequiredTx
    public ApiEntity<MemberAccountDto.MeRes> updateProfile(MemberAccountDto.ProfileReq req, HttpServletResponse response) {
        String name = validName(req.getMemberName());
        String mobile = validMobile(req.getMobile());

        Member member = currentMember();
        member.updateProfile(name, mobile);

        jwtService.jwtPublishWithCookie(response,
                jwtService.convertLoginDtoToJwtDto(new MemberLoginDto(member, customAuthorityUtil)));

        return ApiEntity.of(HttpsStatusCode.SUCCESS, toRes(member));
    }

    /**
     * 비밀번호 변경 (현재 비밀번호 확인)
     */
    @RequiredTx
    public ApiEntity<?> changePassword(MemberAccountDto.PasswordReq req) {
        Member member = currentMember();
        if (!member.hasPassword()) {
            throw business("구글로 가입한 회원은 로그인 화면의 '비밀번호 찾기'로 비밀번호를 설정할 수 있습니다.",
                    HttpsStatusCode.INVALID_PARAMETER);
        }
        if (req.getCurrentPassword() == null
                || !passwordEncoder.matches(req.getCurrentPassword(), member.getLoginPassword())) {
            throw business("현재 비밀번호가 일치하지 않습니다.", HttpsStatusCode.INVALID_PARAMETER);
        }
        if (!PasswordPolicy.isValid(req.getNewPassword())) {
            throw business(PasswordPolicy.MESSAGE, HttpsStatusCode.INVALID_PARAMETER);
        }
        if (req.getNewPassword().equals(req.getCurrentPassword())) {
            throw business("현재 비밀번호와 다른 비밀번호를 입력해주세요.", HttpsStatusCode.INVALID_PARAMETER);
        }

        member.changePassword(passwordEncoder.encode(req.getNewPassword()));
        log.info("[비밀번호 변경] memberId={}", member.getMemberId());
        return ApiEntity.ofMessage(HttpsStatusCode.SUCCESS, "비밀번호가 변경되었습니다.");
    }

    private String validName(String value) {
        String name = value == null ? "" : value.trim();
        if (name.isEmpty()) throw business("이름을 입력해주세요.", HttpsStatusCode.INVALID_PARAMETER);
        if (name.length() > 64) throw business("이름은 64자 이하로 입력해주세요.", HttpsStatusCode.INVALID_PARAMETER);
        return name;
    }

    /**
     * 전화번호 (선택, 해외번호 허용)
     */
    private String validMobile(String value) {
        String mobile = value == null || value.isBlank() ? null : value.trim();
        if (mobile != null && !MOBILE.matcher(mobile).matches()) {
            throw business("전화번호 형식이 올바르지 않습니다.", HttpsStatusCode.INVALID_PARAMETER);
        }
        return mobile;
    }

    private Member currentMember() {
        Long memberId = SecurityUtil.getCurrentMemberId();
        if (memberId == null) throw business(HttpsStatusCode.LOGIN_REQUIRED);
        return memberRepository.findById(memberId)
                .filter(m -> !m.isWithdrawn())
                .orElseThrow(() -> business(HttpsStatusCode.LOGIN_REQUIRED));
    }

    private MemberAccountDto.MeRes toRes(Member member) {
        return MemberAccountDto.MeRes.builder()
                .loginId(member.getLoginId())
                .memberName(member.getMemberName())
                .mobile(member.getMobile())
                .provider(member.getProvider())
                .hasPassword(member.hasPassword())
                .build();
    }
}
