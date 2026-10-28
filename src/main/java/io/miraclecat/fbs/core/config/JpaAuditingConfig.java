package io.miraclecat.fbs.core.config;

import io.miraclecat.fbs.core.auth.dto.JwtDto;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * packageName    : io.miraclecat.fbs.core.config
 * fileName       : JpaAuditingConfig
 * author         : MiracleCat
 * date           : 2023-07-28
 * description    : JPA Auditing (created_id / updated_id = 로그인 회원 ID, 비로그인은 비움)
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {

    @Bean
    public AuditorAware<Long> auditorProvider() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof JwtDto jwtDto) {
                return Optional.ofNullable(jwtDto.getMemberId());
            }
            return Optional.empty();
        };
    }
}
