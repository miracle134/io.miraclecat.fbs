package io.miraclecat.fbs.core.auth.handler;

import io.miraclecat.fbs.core.response.ApiDto;
import io.miraclecat.fbs.core.response.ApiWriter;
import io.miraclecat.fbs.core.response.HttpsStatusCode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * packageName    : io.miraclecat.fbs.auth.handler
 * fileName       : AuthEntryPoint
 * author         : MiracleCat
 * date           : 2023-07-13
 * description    :
 */
@Slf4j
@Component
public class AuthEntryPoint implements AuthenticationEntryPoint {
	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
		log.warn("AuthEntryPoint : {}", authException.getMessage());
		String requestedAjaxHeader = request.getHeader("Ajax-Request");
		if ("Ajax".equals(requestedAjaxHeader)) {
			ApiWriter.writeJson(response, ApiDto.empty(HttpsStatusCode.JWT_EXPIRED));
		} else if (request.getRequestURI().startsWith("/admin")) {
			response.sendRedirect("/admin/login");
		} else {
			response.sendRedirect("/auth/login");
		}
	}
}