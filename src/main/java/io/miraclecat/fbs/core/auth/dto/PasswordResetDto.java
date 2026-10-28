package io.miraclecat.fbs.core.auth.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * packageName    : io.miraclecat.fbs.core.auth.dto
 * fileName       : PasswordResetDto
 * author         : MiracleCat
 * date           : 2026-10-27
 * description    : 비밀번호 재설정 DTO
 */
public class PasswordResetDto {

    /**
     * 재설정 메일 요청
     */
    @Getter
    @Setter
    public static class ForgotReq {
        private String email;
    }

    /**
     * 새 비밀번호 저장
     */
    @Getter
    @Setter
    public static class ResetReq {
        private String token;
        private String newPassword;
    }
}
