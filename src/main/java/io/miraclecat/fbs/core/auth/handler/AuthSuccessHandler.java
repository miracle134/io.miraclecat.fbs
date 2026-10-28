package io.miraclecat.fbs.core.auth.handler;

import io.miraclecat.fbs.core.auth.dto.MemberLoginDto;
import io.miraclecat.fbs.core.response.ApiDto;
import io.miraclecat.fbs.core.response.ApiWriter;
import io.miraclecat.fbs.core.response.HttpsStatusCode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * packageName    : io.miraclecat.fbs.auth.controller
 * fileName       : AuthSuccessHandler
 * author         : MiracleCat 
 * date           : 2023-07-12
 * description    :
 */
@Slf4j
public class AuthSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        MemberLoginDto memberLoginDto = (MemberLoginDto) authentication.getPrincipal();
        log.debug("로그인 성공 getName : {}", authentication.getName());
        log.debug("로그인 성공 getPrincipal : {}", authentication.getPrincipal());

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("role", memberLoginDto.getMember().getRole());

        ApiWriter.writeJson(response, ApiDto.of(HttpsStatusCode.SUCCESS, result));
    }

}
