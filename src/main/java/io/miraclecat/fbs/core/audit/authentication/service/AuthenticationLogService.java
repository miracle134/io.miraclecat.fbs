package io.miraclecat.fbs.core.audit.authentication.service;

import io.miraclecat.fbs.core.audit.authentication.entity.AuthenticationLog;
import io.miraclecat.fbs.core.audit.authentication.repository.AuthenticationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * packageName    : io.miraclecat.fbs.core.audit.authentication.service
 * fileName       : AuthenticationLogService
 * author         : MiracleCat
 * date           : 2025-09-23
 * description    :
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AuthenticationLogService {

    private final AuthenticationLogRepository authenticationLogRepository;

    public void logAuthentication(Long memberId, String loginId, String memberName,
                                 AuthenticationLog.LoginType loginType,
                                 String ipAddress, String userAgent,
                                 boolean success, String failureReason) {
        try {
            AuthenticationLog authenticationLog = AuthenticationLog.createAuthenticationLog()
                .memberId(memberId)
                .loginId(loginId)
                .memberName(memberName)
                .loginType(loginType)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .isSuccess(success)
                .failureReason(failureReason)
                .build();

            authenticationLogRepository.save(authenticationLog);
        } catch (Exception e) {
            log.error("인증 로그 저장 실패", e);
        }
    }

    public long getFailedLoginCount(String loginId, int minutes) {
        LocalDateTime since = LocalDateTime.now().minusMinutes(minutes);
        return authenticationLogRepository.countFailedAttemptsSince(loginId,
            AuthenticationLog.LoginType.LOGIN_FAIL, since);
    }

}