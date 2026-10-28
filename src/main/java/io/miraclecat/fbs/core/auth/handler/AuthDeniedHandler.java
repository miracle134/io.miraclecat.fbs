package io.miraclecat.fbs.core.auth.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * packageName    : io.miraclecat.fbs.auth.handler
 * fileName       : AuthDeniedHandler
 * author         : MiracleCat
 * date           : 2023-07-13
 * description    :
 */
@Slf4j
@Component
public class AuthDeniedHandler implements AccessDeniedHandler {
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {
        log.warn("AuthDeniedHandler: {}", accessDeniedException.getMessage());

        request.getRequestDispatcher("/denied").forward(request, response);
    }
}