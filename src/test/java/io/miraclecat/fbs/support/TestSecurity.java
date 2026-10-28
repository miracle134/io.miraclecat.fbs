package io.miraclecat.fbs.support;

import io.miraclecat.fbs.core.auth.AuthRole;
import io.miraclecat.fbs.core.auth.dto.JwtDto;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

/**
 * 테스트용 로그인 회원 설정 (SecurityUtil.getCurrentMemberId() 가 읽는 SecurityContext)
 */
public final class TestSecurity {

    private TestSecurity() {
    }

    public static void loginAs(Long memberId, AuthRole role) {
        JwtDto principal = JwtDto.builder()
                .memberId(memberId)
                .loginId("member" + memberId + "@test.com")
                .name("테스트" + memberId)
                .role(role)
                .build();
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }

    public static void clear() {
        SecurityContextHolder.clearContext();
    }
}
