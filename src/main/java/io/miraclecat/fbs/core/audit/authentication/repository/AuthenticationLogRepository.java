package io.miraclecat.fbs.core.audit.authentication.repository;

import io.miraclecat.fbs.core.audit.authentication.entity.AuthenticationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

/**
 * packageName    : io.miraclecat.fbs.core.audit.authentication.repository
 * fileName       : AuthenticationLogRepository
 * author         : MiracleCat 
 * date           : 2025-09-23
 * description    :
 */
public interface AuthenticationLogRepository extends JpaRepository<AuthenticationLog, Long> {

    @Query("SELECT COUNT(a) FROM AuthenticationLog a WHERE a.loginId = :loginId " +
            "AND a.loginType = :loginType AND a.createdAt >= :since")
    long countFailedAttemptsSince(@Param("loginId") String loginId,
                                  @Param("loginType") AuthenticationLog.LoginType loginType,
                                  @Param("since") LocalDateTime since);


}