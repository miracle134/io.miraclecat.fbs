package io.miraclecat.fbs.core.auth.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * packageName    : io.miraclecat.fbs.core.auth.handler
 * fileName       : OAuth2FailureHandler
 * author         : MiracleCat
 * date           : 2026-10-26
 * description    : 소셜 로그인 실패 (사용자 동의 취소, 토큰 교환 실패 등)
 */
@Slf4j
@Component
public class OAuth2FailureHandler implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        log.warn("OAuth2 인증 실패: {}", exception.getMessage());
        response.sendRedirect("/auth/login?error=oauth_failed");
    }
}
