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
 * packageName    : io.miraclecat.fbs.core.auth.handler
 * fileName       : ExternalApiEntryPointHandler
 * author         : MiracleCat 
 * date           : 2025-07-11
 * description    :
 */
@Slf4j
@Component
public class ExternalApiEntryPointHandler implements AuthenticationEntryPoint {
	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
		log.warn("ExternalApiEntryPointHandler : {}", authException.getMessage());
		
		ApiWriter.writeJson(response, ApiDto.empty(HttpsStatusCode.INVALID_API_KEY));
	}
}