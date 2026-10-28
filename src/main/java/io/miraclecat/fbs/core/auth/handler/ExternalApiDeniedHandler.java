package io.miraclecat.fbs.core.auth.handler;

import io.miraclecat.fbs.core.response.ApiDto;
import io.miraclecat.fbs.core.response.ApiWriter;
import io.miraclecat.fbs.core.response.HttpsStatusCode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * packageName    : io.miraclecat.fbs.core.auth.handler
 * fileName       : ExternalApiDeniedHandler
 * author         : MiracleCat
 * date           : 2025-07-11
 * description    :
 */
@Slf4j
@Component
public class ExternalApiDeniedHandler implements AccessDeniedHandler {
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {
        log.warn("ExternalApiDeniedHandler: {}", accessDeniedException.getMessage());

        ApiWriter.writeJson(response, ApiDto.empty(HttpsStatusCode.ACCESS_DENIED));
    }
}