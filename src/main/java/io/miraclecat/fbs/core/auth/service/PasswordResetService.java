package io.miraclecat.fbs.core.auth.service;

import io.miraclecat.fbs.api.member.domain.entity.Member;
import io.miraclecat.fbs.api.member.domain.repository.MemberRepository;
import io.miraclecat.fbs.core.config.RequiredTx;
import io.miraclecat.fbs.core.mail.MailService;
import io.miraclecat.fbs.core.response.HttpsStatusCode;
import io.miraclecat.fbs.core.utils.security.PasswordPolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

import static io.miraclecat.fbs.core.exception.GlobalException.business;

/**
 * packageName    : io.miraclecat.fbs.core.auth.service
 * fileName       : PasswordResetService
 * author         : MiracleCat
 * date           : 2026-10-27
 * description    : 비밀번호 재설정 (메일 링크 + Redis 1회용 토큰)
 *
 * pw-reset:token:{token}     → memberId   (30분, 사용 시 삭제)
 * pw-reset:member:{memberId} → token      (새 요청 시 이전 토큰 무효화)
 * pw-reset:cooldown:{email}  → 1          (1분 재요청 제한)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final String TOKEN_KEY = "pw-reset:token:";
    private static final String MEMBER_KEY = "pw-reset:member:";
    private static final String COOLDOWN_KEY = "pw-reset:cooldown:";
    private static final Duration TOKEN_TTL = Duration.ofMinutes(30);
    private static final Duration COOLDOWN_TTL = Duration.ofMinutes(1);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final MemberRepository memberRepository;
    private final StringRedisTemplate redisTemplate;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;

    /**
     * 재설정 메일 요청
     * 가입 여부를 노출하지 않도록 결과와 상관없이 같은 응답을 준다
     */
    public void requestReset(String email, String baseUrl) {
        if (email == null || email.isBlank()) {
            throw business("이메일을 입력해주세요.", HttpsStatusCode.INVALID_PARAMETER);
        }
        String normalized = email.trim();

        Boolean first = redisTemplate.opsForValue().setIfAbsent(COOLDOWN_KEY + normalized, "1", COOLDOWN_TTL);
        if (!Boolean.TRUE.equals(first)) {
            log.info("[비밀번호 재설정] 재요청 제한: {}", normalized);
            return;
        }

        Optional<Member> found = memberRepository.findByLoginIdAndWithdrawnAt(normalized);
        if (found.isEmpty()) {
            log.info("[비밀번호 재설정] 미가입 이메일 요청: {}", normalized);
            return;
        }
        Member member = found.get();

        // 이전 토큰 무효화
        String memberKey = MEMBER_KEY + member.getMemberId();
        String oldToken = redisTemplate.opsForValue().get(memberKey);
        if (oldToken != null) redisTemplate.delete(TOKEN_KEY + oldToken);

        String token = newToken();
        redisTemplate.opsForValue().set(TOKEN_KEY + token, String.valueOf(member.getMemberId()), TOKEN_TTL);
        redisTemplate.opsForValue().set(memberKey, token, TOKEN_TTL);

        mailService.send(member.getLoginId(), "[Flight Booking] 비밀번호 재설정 안내", "password-reset", Map.of(
                "name", member.getMemberName() != null ? member.getMemberName() : "회원",
                "resetUrl", baseUrl + "/auth/password/reset?token=" + token,
                "expireMinutes", TOKEN_TTL.toMinutes()
        ));
    }

    /**
     * 링크 유효 여부 (재설정 화면 진입 시)
     */
    public boolean isValidToken(String token) {
        return token != null && !token.isBlank() && Boolean.TRUE.equals(redisTemplate.hasKey(TOKEN_KEY + token));
    }

    /**
     * 새 비밀번호 저장 (토큰은 1회용)
     */
    @RequiredTx
    public void resetPassword(String token, String newPassword) {
        if (!PasswordPolicy.isValid(newPassword)) {
            throw business(PasswordPolicy.MESSAGE, HttpsStatusCode.INVALID_PARAMETER);
        }
        if (token == null || token.isBlank()) {
            throw business("유효하지 않은 링크입니다.", HttpsStatusCode.INVALID_PARAMETER);
        }

        String memberId = redisTemplate.opsForValue().getAndDelete(TOKEN_KEY + token);
        if (memberId == null) {
            throw business("링크가 만료되었거나 이미 사용되었습니다. 다시 요청해주세요.", HttpsStatusCode.INVALID_PARAMETER);
        }
        redisTemplate.delete(MEMBER_KEY + memberId);

        Member member = memberRepository.findById(Long.valueOf(memberId))
                .filter(m -> !m.isWithdrawn())
                .orElseThrow(() -> business("회원 정보를 찾을 수 없습니다.", HttpsStatusCode.RESOURCE_NOT_FOUND));
        member.changePassword(passwordEncoder.encode(newPassword));
        log.info("[비밀번호 재설정 완료] memberId={}", memberId);
    }

    private String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
