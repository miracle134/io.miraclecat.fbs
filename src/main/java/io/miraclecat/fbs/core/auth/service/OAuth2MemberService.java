package io.miraclecat.fbs.core.auth.service;

import io.miraclecat.fbs.api.member.domain.entity.Member;
import io.miraclecat.fbs.api.member.domain.repository.MemberRepository;
import io.miraclecat.fbs.core.auth.AuthRole;
import io.miraclecat.fbs.core.config.RequiredTx;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * packageName    : io.miraclecat.fbs.core.auth.service
 * fileName       : OAuth2MemberService
 * author         : MiracleCat
 * date           : 2026-10-26
 * description    : 소셜 로그인 회원 조회 / 연결 / 생성
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OAuth2MemberService {

    private final MemberRepository memberRepository;

    /**
     * 1) provider + providerId 로 조회
     * 2) 없으면 같은 이메일 회원에 연결 (이메일 인증된 경우만)
     * 3) 없으면 신규 생성
     */
    @RequiredTx
    public Member resolveMember(String provider, String providerId, String email,
                                Boolean emailVerified, String name) {
        Optional<Member> linked = memberRepository.findByProviderAndProviderId(provider, providerId);
        if (linked.isPresent()) {
            Member member = linked.get();
            if (member.isWithdrawn()) throw new OAuth2LoginFailException("withdrawn");
            if (member.isSuspended()) throw new OAuth2LoginFailException("suspended");
            return member;
        }

        if (email == null || email.isBlank()) throw new OAuth2LoginFailException("no_email");

        Optional<Member> sameEmail = memberRepository.findByLoginId(email);
        if (sameEmail.isPresent()) {
            Member member = sameEmail.get();
            if (member.isWithdrawn()) throw new OAuth2LoginFailException("withdrawn");
            if (member.isSuspended()) throw new OAuth2LoginFailException("suspended");
            if (!Boolean.TRUE.equals(emailVerified)) throw new OAuth2LoginFailException("email_unverified");
            if (member.getProviderId() != null) throw new OAuth2LoginFailException("already_linked");

            member.linkOAuth(provider, providerId);
            log.info("소셜 계정 연결: memberId={}, provider={}", member.getMemberId(), provider);
            return member;
        }

        Member member = Member.createOAuthMember(email, name, AuthRole.ROLE_USER, provider, providerId);
        memberRepository.save(member);
        log.info("소셜 회원 가입: memberId={}, provider={}", member.getMemberId(), provider);
        return member;
    }

    @Getter
    public static class OAuth2LoginFailException extends RuntimeException {
        private final String errorCode;

        public OAuth2LoginFailException(String errorCode) {
            super(errorCode);
            this.errorCode = errorCode;
        }
    }
}
