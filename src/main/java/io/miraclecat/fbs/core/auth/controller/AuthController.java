package io.miraclecat.fbs.core.auth.controller;

import io.miraclecat.fbs.core.audit.authentication.entity.AuthenticationLog;
import io.miraclecat.fbs.core.audit.authentication.service.AuthenticationLogService;
import io.miraclecat.fbs.core.audit.authentication.util.AuthenticationLogUtil;
import io.miraclecat.fbs.core.auth.dto.JwtDto;
import io.miraclecat.fbs.core.utils.web.RedirectUtil;
import io.miraclecat.fbs.core.utils.web.SessionAndCookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;

/**
 * packageName    : io.miraclecat.fbs.auth.controller
 * fileName       : AuthController
 * author         : MiracleCat
 * date           : 2023-07-07
 * description    :
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class AuthController {
	
	private final AuthenticationLogService authenticationLogService;

	/**
	 * 로그인 페이지
	 *
	 * @return ModelAndView
	 */
	@GetMapping("/auth/login")
	public ModelAndView login(Authentication authentication,
							  @RequestParam(value = "redirect", required = false) String redirect,
							  HttpServletResponse response) throws IOException {
		ModelAndView mv = new ModelAndView("/page/member/auth/login");
		
		if (authentication != null) {
			log.debug("로그인 성공 isAuthenticated : {}", authentication.isAuthenticated());
			response.sendRedirect(RedirectUtil.orDefault(redirect));
		}
		return mv;
	}

	/**
	 * 로그아웃
	 *
	 * @return ModelAndView
	 */
	@GetMapping("/auth/logout")
	public void logout(@AuthenticationPrincipal JwtDto jwtDto,
                       @RequestParam(value = "redirect", required = false) String redirect,
                       HttpServletRequest request,
                       HttpServletResponse response) throws IOException {

        String ipAddress = AuthenticationLogUtil.getClientIpAddress(request);
        String userAgent = request != null ? request.getHeader("User-Agent") : null;

		// JWT 쿠키 삭제
		SessionAndCookieUtil.removeCookie(response, "Auth-Token-Access");
		SessionAndCookieUtil.removeCookie(response, "Auth-Token-Refresh");

        // 로그아웃 로그
        if (jwtDto != null) {
            authenticationLogService.logAuthentication(
                jwtDto.getMemberId(), jwtDto.getLoginId(), jwtDto.getName(),
                AuthenticationLog.LoginType.LOGOUT,
                ipAddress, userAgent, true, null);
        }

		// 로그인 페이지로 리턴 (관리자 로그인 등 내부 경로 지정 가능)
		response.sendRedirect("/admin/login".equals(redirect) ? redirect : "/auth/login");
	}

}
