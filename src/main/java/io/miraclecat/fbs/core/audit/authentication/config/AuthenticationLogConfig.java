package io.miraclecat.fbs.core.audit.authentication.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * packageName    : io.miraclecat.fbs.core.audit.authentication.config
 * fileName       : AuthenticationLogConfig
 * author         : MiracleCat 
 * date           : 2025-09-23
 * description    :
 */
@Configuration
@EnableAsync
public class AuthenticationLogConfig {
    @Bean
    public TaskExecutor authenticationLogTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("auth-log-");
        executor.initialize();
        return executor;
    }
}