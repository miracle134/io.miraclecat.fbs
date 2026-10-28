/**
 * packageName    : io.miraclecat.fbs.core.config
 * fileName       : WebSocketConfig
 * author         : MiracleCat
 * date           : 2026-10-24
 * description    : STOMP 웹소켓 설정
 *                  - 접속 엔드포인트 : /ws
 *                  - 구독(서버 → 클라이언트) : /topic/**  (좌석: /topic/seats/{inventoryId})
 *                  - 발행(클라이언트 → 서버) : /app/**    (현재 미사용, 선점/해제는 REST로 처리)
 *                  - SimpleBroker는 서버 메모리 기반 → 다중 서버 시 Redis Pub/Sub 또는 외부 브로커로 교체 필요
 */
package io.miraclecat.fbs.core.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private static final long HEARTBEAT_MS = 10_000L;

    @Value("${app.base-url}")
    private String baseUrl;

    /**
     * 허용 Origin: 서비스 도메인(app.base-url)만
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins(baseUrl);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 유휴 연결이 nginx 등 프록시 타임아웃으로 끊기지 않도록 주기적으로 신호를 주고받음
        ThreadPoolTaskScheduler heartbeatScheduler = new ThreadPoolTaskScheduler();
        heartbeatScheduler.setPoolSize(1);
        heartbeatScheduler.setThreadNamePrefix("ws-heartbeat-");
        heartbeatScheduler.initialize();

        registry.enableSimpleBroker("/topic")
                .setHeartbeatValue(new long[]{HEARTBEAT_MS, HEARTBEAT_MS})
                .setTaskScheduler(heartbeatScheduler);

        registry.setApplicationDestinationPrefixes("/app");
    }
}
