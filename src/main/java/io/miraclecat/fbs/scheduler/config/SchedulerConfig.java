package io.miraclecat.fbs.scheduler.config;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.sql.DataSource;

/**
 * packageName    : io.miraclecat.fbs.scheduler.config
 * fileName       : SchedulerConfig
 * author         : jiho
 * date           : 26. 6. 2.
 * description    :
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 26. 6. 2.        jiho       최초 생성
 */
@Configuration
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "PT3M")
public class SchedulerConfig {

    /**
     * ShedLock - JdbcTemplate 기반 (shedlock 테이블 사용)
     * 추후 Redis 기반으로 전환 가능:
     * RedisLockProvider(redisConnectionFactory)
     */
    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        return new JdbcTemplateLockProvider(
                JdbcTemplateLockProvider.Configuration.builder()
                        .withJdbcTemplate(new JdbcTemplate(dataSource))
                        .usingDbTime()
                        .build()
        );
    }

}