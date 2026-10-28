package io.miraclecat.fbs.core.auth.controller;

import io.miraclecat.fbs.core.auth.dto.PasswordResetDto;
import io.miraclecat.fbs.core.auth.service.PasswordResetService;
import io.miraclecat.fbs.core.response.ApiEntity;
import io.miraclecat.fbs.core.response.HttpsStatusCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

/**
 * packageName    : io.miraclecat.fbs.core.auth.controller
 * fileName       : PasswordResetController
 * author         : MiracleCat
 * date           : 2026-10-27
 * description    : 비밀번호 재설정 (/auth/** 는 비로그인 허용)
 */
@RestController
@RequiredArgsConstructor
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    @Value("${app.base-url}")
    private String baseUrl;

    /**
     * 재설정 메일 요청 (링크는 요청 Host가 아닌 설정 도메인으로 생성)
     */
    @PostMapping("/auth/password/forgot")
    public ApiEntity<?> forgot(@RequestBody PasswordResetDto.ForgotReq req) {
        passwordResetService.requestReset(req.getEmail(), baseUrl);
        return ApiEntity.ofMessage(HttpsStatusCode.SUCCESS,
                "가입된 이메일이면 비밀번호 재설정 메일이 발송됩니다. 메일함을 확인해주세요.");
    }

    /**
     * 재설정 화면 (메일 링크)
     */
    @GetMapping("/auth/password/reset")
    public ModelAndView resetPage(@RequestParam(value = "token", required = false) String token) {
        ModelAndView mv = new ModelAndView("/page/member/auth/password-reset");
        boolean valid = passwordResetService.isValidToken(token);
        mv.addObject("tokenValid", valid);
        mv.addObject("token", valid ? token : "");
        return mv;
    }

    /**
     * 새 비밀번호 저장
     */
    @PostMapping("/auth/password/reset")
    public ApiEntity<?> reset(@RequestBody PasswordResetDto.ResetReq req) {
        passwordResetService.resetPassword(req.getToken(), req.getNewPassword());
        return ApiEntity.ofMessage(HttpsStatusCode.SUCCESS, "비밀번호가 변경되었습니다. 새 비밀번호로 로그인해주세요.");
    }
}
