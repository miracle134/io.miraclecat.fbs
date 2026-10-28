package io.miraclecat.fbs.api.member.dto;

import lombok.*;

/**
 * packageName    : io.miraclecat.fbs.api.member.dto
 * fileName       : MemberAccountDto
 * author         : MiracleCat
 * date           : 2026-10-27
 * description    : 회원가입 / 내 정보 조회 / 수정 / 비밀번호 변경
 */
public class MemberAccountDto {

    /**
     * 회원가입 (아이디 = 이메일)
     */
    @Getter
    @Setter
    public static class SignupReq {
        private String loginId;
        private String loginPassword;
        private String memberName;
        private String mobile;
    }

    /**
     * 내 정보
     * hasPassword : 구글로만 가입한 회원은 false (비밀번호 변경 숨김)
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MeRes {
        private String loginId;
        private String memberName;
        private String mobile;
        private String provider;
        private boolean hasPassword;
    }

    /**
     * 이름 / 전화번호 수정
     */
    @Getter
    @Setter
    public static class ProfileReq {
        private String memberName;
        private String mobile;
    }

    /**
     * 비밀번호 변경
     */
    @Getter
    @Setter
    public static class PasswordReq {
        private String currentPassword;
        private String newPassword;
    }
}
