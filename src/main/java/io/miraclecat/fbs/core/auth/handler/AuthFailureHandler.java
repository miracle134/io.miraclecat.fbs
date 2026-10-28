package io.miraclecat.fbs.core.auth.handler;

import io.miraclecat.fbs.core.response.ApiDto;
import io.miraclecat.fbs.core.response.ApiWriter;
import io.miraclecat.fbs.core.response.HttpsStatusCode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import java.io.IOException;

/**
 * packageName    : io.miraclecat.fbs.auth
 * fileName       : AuthFailureHandler
 * author         : MiracleCat
 * date           : 2023-07-12
 * description    :
 */
@Slf4j
public class AuthFailureHandler implements AuthenticationFailureHandler {
	
	@Override
	public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response
		, AuthenticationException exception) throws IOException, ServletException {
		if (exception instanceof DisabledException) { // 관리자 정지 (비밀번호 확인 후에만 발생)
			log.warn("DisabledException : {}", exception.getMessage());
			ApiWriter.writeJson(response, ApiDto.of(HttpsStatusCode.ID_PW_NOT_FOUND, "이용이 정지된 계정입니다. 고객센터에 문의해주세요.", null));
		} else if (exception instanceof BadCredentialsException) { // 기본적으로 여기 Exception 으로 캐치됨 (Secure Coding; 보안문제)
			log.warn("BadCredentialsException : {}", exception.getMessage());
			ApiWriter.writeJson(response, ApiDto.empty(HttpsStatusCode.ID_PW_NOT_FOUND));
		} else if (exception instanceof InternalAuthenticationServiceException) {
			log.warn("InternalAuthenticationServiceException : {}", exception.getMessage());
			ApiWriter.writeJson(response, ApiDto.empty(HttpsStatusCode.ID_PW_NOT_FOUND));
		} else if (exception instanceof UsernameNotFoundException) {
			log.warn("UsernameNotFoundException : {}", exception.getMessage());
			ApiWriter.writeJson(response, ApiDto.empty(HttpsStatusCode.ID_PW_NOT_FOUND));
		} else {
			log.warn("AuthException : {}", exception.getMessage());
			ApiWriter.writeJson(response, ApiDto.empty(HttpsStatusCode.ID_PW_NOT_FOUND));
		}
	}
	
}
