package io.miraclecat.fbs.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * packageName    : io.miraclecat.fbs.core.config
 * fileName       : AsyncConfig
 * author         : MiracleCat
 * date           : 2026-10-26
 * description    : 비동기 실행 설정 (메일 발송)
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    public static final String MAIL_EXECUTOR = "mailExecutor";

    @Bean(name = MAIL_EXECUTOR)
    public Executor mailExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("mail-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);
        executor.initialize();
        return executor;
    }
}
