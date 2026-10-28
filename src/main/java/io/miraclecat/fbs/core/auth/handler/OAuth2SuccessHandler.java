package io.miraclecat.fbs.core.auth.handler;

import io.miraclecat.fbs.api.member.domain.entity.Member;
import io.miraclecat.fbs.core.audit.authentication.entity.AuthenticationLog;
import io.miraclecat.fbs.core.audit.authentication.service.AuthenticationLogService;
import io.miraclecat.fbs.core.audit.authentication.util.AuthenticationLogUtil;
import io.miraclecat.fbs.core.auth.config.CustomAuthorityUtil;
import io.miraclecat.fbs.core.auth.config.RedisOAuth2AuthorizationRequestRepository;
import io.miraclecat.fbs.core.auth.dto.JwtDto;
import io.miraclecat.fbs.core.auth.dto.MemberLoginDto;
import io.miraclecat.fbs.core.auth.service.JwtService;
import io.miraclecat.fbs.core.auth.service.OAuth2MemberService;
import io.miraclecat.fbs.core.auth.service.OAuth2MemberService.OAuth2LoginFailException;
import io.miraclecat.fbs.core.utils.web.RedirectUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Locale;

/**
 * packageName    : io.miraclecat.fbs.core.auth.handler
 * fileName       : OAuth2SuccessHandler
 * author         : MiracleCat
 * date           : 2026-10-26
 * description    : 소셜 로그인 성공 → 회원 조회/연결/생성 → 자체 JWT 발급
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final OAuth2MemberService oAuth2MemberService;
    private final JwtService jwtService;
    private final CustomAuthorityUtil customAuthorityUtil;
    private final AuthenticationLogService authenticationLogService;
    private final RedisOAuth2AuthorizationRequestRepository authorizationRequestRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        String redirect = authorizationRequestRepository.popRedirect(request);

        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
        String provider = token.getAuthorizedClientRegistrationId().toUpperCase(Locale.ROOT);
        OidcUser user = (OidcUser) token.getPrincipal();

        String ipAddress = AuthenticationLogUtil.getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");

        Member member;
        try {
            member = oAuth2MemberService.resolveMember(
                    provider, user.getSubject(), user.getEmail(), user.getEmailVerified(), user.getFullName());
        } catch (OAuth2LoginFailException e) {
            log.warn("소셜 로그인 실패: provider={}, email={}, reason={}", provider, user.getEmail(), e.getErrorCode());
            logSafely(null, user.getEmail(), null, AuthenticationLog.LoginType.LOGIN_FAIL,
                    ipAddress, userAgent, false, provider + ":" + e.getErrorCode());
            response.sendRedirect("/auth/login?error=" + e.getErrorCode());
            return;
        }

        JwtDto jwtDto = jwtService.convertLoginDtoToJwtDto(new MemberLoginDto(member, customAuthorityUtil));
        jwtService.jwtPublishWithCookie(response, jwtDto);

        logSafely(jwtDto.getMemberId(), jwtDto.getLoginId(), jwtDto.getName(), AuthenticationLog.LoginType.LOGIN,
                ipAddress, userAgent, true, null);
        log.info("소셜 로그인 성공: {}, provider={}, IP: {}", jwtDto.getLoginId(), provider, ipAddress);

        response.sendRedirect(RedirectUtil.orDefault(redirect));
    }

    private void logSafely(Long memberId, String loginId, String name, AuthenticationLog.LoginType type,
                           String ip, String userAgent, boolean success, String reason) {
        try {
            authenticationLogService.logAuthentication(memberId, loginId, name, type, ip, userAgent, success, reason);
        } catch (Exception e) {
            log.error("소셜 로그인 로그 처리 중 오류", e);
        }
    }
}
