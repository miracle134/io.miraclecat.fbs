package io.miraclecat.fbs.core.auth.filter;

import io.miraclecat.fbs.core.audit.authentication.entity.AuthenticationLog;
import io.miraclecat.fbs.core.audit.authentication.service.AuthenticationLogService;
import io.miraclecat.fbs.core.audit.authentication.util.AuthenticationLogUtil;
import io.miraclecat.fbs.core.auth.dto.JwtDto;
import io.miraclecat.fbs.core.auth.dto.MemberLoginDto;
import io.miraclecat.fbs.core.auth.dto.request.LoginReqDto;
import io.miraclecat.fbs.core.auth.handler.AuthFailureHandler;
import io.miraclecat.fbs.core.auth.handler.AuthSuccessHandler;
import io.miraclecat.fbs.core.auth.service.JwtService;
import io.miraclecat.fbs.core.utils.data.ObjectMapperEnum;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * packageName    : io.miraclecat.fbs.auth.filter
 * fileName       : JwtAuthenticationFilter
 * author         : MiracleCat
 * date           : 2023-07-12
 * description    : 인증 및 인가(JWT)
 */
@Slf4j
public class JwtAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AuthenticationLogService authenticationLogService;

    public JwtAuthenticationFilter(AuthenticationManager authenticationManager, JwtService jwtService, AuthenticationLogService authenticationLogService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.authenticationLogService = authenticationLogService;
        this.setFilterProcessesUrl("/auth/loginProcess");
        this.setAuthenticationSuccessHandler(new AuthSuccessHandler());
        this.setAuthenticationFailureHandler(new AuthFailureHandler());
    }
    
    /**
     * 로그인 시도 (JSON 요청 바디에서 id/password 추출)
     */
    @Override
    public Authentication attemptAuthentication(HttpServletRequest request,
                                                HttpServletResponse response) throws AuthenticationException {
        try {
            ObjectMapper objectMapper = ObjectMapperEnum.JSON_DEFAULT.getMapper();
            LoginReqDto loginReqDto = objectMapper.readValue(request.getInputStream(), LoginReqDto.class);

            request.setAttribute("ATTEMPTED_LOGIN_ID", loginReqDto.getLoginId());

            UsernamePasswordAuthenticationToken authRequest =
                    new UsernamePasswordAuthenticationToken(loginReqDto.getLoginId(),
                        loginReqDto.getLoginPassword());
    
            Authentication result = authenticationManager.authenticate(authRequest);

            // 비밀번호 확인 후 정지 여부 검사 (비밀번호가 틀리면 정지 여부를 노출하지 않음)
            if (result.getPrincipal() instanceof MemberLoginDto loginDto && loginDto.getMember().isSuspended()) {
                throw new DisabledException("정지된 계정");
            }
            return result;
        } catch (IOException e) {
            throw new RuntimeException("Invalid login request", e);
        }
    }
    
    /**
     * 인증 성공 시 Access + Refresh 토큰을 JSON으로 응답
     */
    @SneakyThrows
    @Override
    protected void successfulAuthentication(HttpServletRequest request,
                                            HttpServletResponse response,
                                            FilterChain chain,
                                            Authentication authResult) throws ServletException, IOException {
        MemberLoginDto memberLoginDto = (MemberLoginDto) authResult.getPrincipal();
        JwtDto jwtDto = jwtService.convertLoginDtoToJwtDto(memberLoginDto);

        // Token 발급
        jwtService.jwtPublishWithCookie(response, jwtDto);

        try {

            // 사용자 정보 추출
            String ipAddress = AuthenticationLogUtil.getClientIpAddress(request);
            String userAgent = request != null ? request.getHeader("User-Agent") : null;

            authenticationLogService.logAuthentication(
                jwtDto.getMemberId(),
                jwtDto.getLoginId(),
                jwtDto.getName(),
                AuthenticationLog.LoginType.LOGIN,
                ipAddress, userAgent, true, null);

            log.info("로그인 성공: {}, IP: {}", jwtDto.getLoginId(), ipAddress);
        } catch (Exception e) {
            log.error("로그인 성공 로그 처리 중 오류", e);
        }

        this.getSuccessHandler().onAuthenticationSuccess(request, response, authResult);  // 추가
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request,
                                            HttpServletResponse response,
                                            AuthenticationException failed) throws ServletException, IOException {
        try {
            String loginId = (String) request.getAttribute("ATTEMPTED_LOGIN_ID"); // 로그인 시도한 ID
            String ipAddress = AuthenticationLogUtil.getClientIpAddress(request);
            String userAgent = request.getHeader("User-Agent");
            String failureReason = AuthenticationLogUtil.truncateMessage(failed.getMessage(), 197);

            authenticationLogService.logAuthentication(
                null, // 실패 시 memberId는 null
                loginId,
                null, // 실패 시 memberName도 null
                AuthenticationLog.LoginType.LOGIN_FAIL,
                ipAddress, userAgent, false, failureReason);

            log.warn("로그인 실패: {}, IP: {}, 사유: {}", loginId, ipAddress, failureReason);
        } catch (Exception e) {
            log.error("로그인 실패 로그 처리 중 오류", e);
        }

        super.unsuccessfulAuthentication(request, response, failed);
    }

}
