package io.miraclecat.fbs.core.config;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.api.StatefulConnection;
import io.miraclecat.fbs.core.utils.data.ObjectMapperEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.jsontype.PolymorphicTypeValidator;

import java.time.Duration;

/**
 * packageName    : io.miraclecat.fbs.core.config
 * fileName       : RedisConfig
 * author         : MiracleCat
 * date           : 2025-10-21
 * description    :
 *
 * 주요 기능:
 * - Lettuce Connection Factory 설정
 * - RedisTemplate 설정
 * - JSON 직렬화 설정
 * - Thread-safe 자동 지원
 * - Jackson 3 (Spring Data Redis 4.x) 직렬화 적용
 */
@Slf4j
@Configuration
@EnableCaching
public class RedisConfig {

    @Value("${spring.data.redis.sentinel.master:}")
    private String master;

    @Value("${spring.data.redis.sentinel.nodes:}")
    private String sentinelNodes;

    @Value("${spring.data.redis.host:localhost}")
    private String host;

    @Value("${spring.data.redis.port:6379}")
    private int port;

    @Value("${spring.data.redis.password:}")
    private String password;

    @Value("${spring.data.redis.database:0}")
    private int database;

    @Value("${spring.data.redis.timeout:100}")
    private long timeout;

    @Value("${spring.data.redis.connect-timeout:100}")
    private long connectTimeout;

    @Value("${spring.data.redis.lettuce.pool.max-active:8}")
    private int maxActive;

    @Value("${spring.data.redis.lettuce.pool.max-idle:8}")
    private int maxIdle;

    @Value("${spring.data.redis.lettuce.pool.min-idle:0}")
    private int minIdle;

    @Value("${spring.data.redis.lettuce.pool.max-wait:200}")
    private long maxWait;

    @Value("${spring.data.redis.lettuce.pool.time-between-eviction-runs:60}")
    private long timeBetweenEvictionRuns;

    /**
     * Lettuce Connection Factory
     * Thread-safe하며 Connection Pool 관리 불필요
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        // Pool 설정
        GenericObjectPoolConfig<StatefulConnection<?, ?>> poolConfig = new GenericObjectPoolConfig<>();
        poolConfig.setMaxTotal(maxActive);
        poolConfig.setMaxIdle(maxIdle);
        poolConfig.setMinIdle(minIdle);
        poolConfig.setMaxWait(Duration.ofMillis(maxWait));
        poolConfig.setTimeBetweenEvictionRuns(Duration.ofMillis(timeBetweenEvictionRuns));

        // Lettuce 클라이언트 옵션 설정
        LettuceClientConfiguration clientConfig = LettucePoolingClientConfiguration.builder()
                .commandTimeout(Duration.ofMillis(timeout))
                .poolConfig(poolConfig)
                .clientOptions(ClientOptions.builder()
                        .socketOptions(SocketOptions.builder()
                                .connectTimeout(Duration.ofMillis(connectTimeout))
                                .build())
                        .disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS)
                        .build())
                .build();

        LettuceConnectionFactory factory;

        // Sentinel 설정이 있으면 Sentinel 모드
        if (master != null && !master.isEmpty() && sentinelNodes != null && !sentinelNodes.isEmpty()) {
            log.info("Redis Sentinel 모드로 초기화 - master: {}", master);

            RedisSentinelConfiguration config = new RedisSentinelConfiguration()
                    .master(master);

            for (String node : sentinelNodes.split(",")) {
                String[] parts = node.trim().split(":");
                config.sentinel(parts[0], Integer.parseInt(parts[1]));
                log.info("Sentinel 노드 추가: {}:{}", parts[0], parts[1]);
            }

            config.setDatabase(database);
            if (password != null && !password.isBlank()) {
                config.setPassword(password);
            }

            factory = new LettuceConnectionFactory(config, clientConfig);
        }
        // Sentinel 설정이 없으면 Standalone 모드
        else {
            log.info("Redis Standalone 모드로 초기화 - {}:{}", host, port);

            RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
            config.setHostName(host);
            config.setPort(port);
            config.setDatabase(database);

            if (password != null && !password.isEmpty()) {
                config.setPassword(password);
            }

            factory = new LettuceConnectionFactory(config, clientConfig);
        }

        log.info("Redis 연결 설정 완료 - timeout: {}ms, pool: {}/{}",
                timeout, maxActive, maxIdle);

        return factory;
    }

    /**
     * String 전용 RedisTemplate
     * 가장 많이 사용하는 기본 템플릿
     */
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }

    /**
     * Object 직렬화용 RedisTemplate
     * ObjectMapperEnum의 JSON_DEFAULT 사용
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Key Serializer
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        // Value Serializer (ObjectMapperEnum 사용)
        ObjectMapper objectMapper = ObjectMapperEnum.JSON_DEFAULT.getMapper();
        JacksonJsonRedisSerializer<Object> jacksonJsonRedisSerializer =
                new JacksonJsonRedisSerializer<>(objectMapper, Object.class);

        template.setValueSerializer(jacksonJsonRedisSerializer);
        template.setHashValueSerializer(jacksonJsonRedisSerializer);

        template.afterPropertiesSet();
        return template;
    }

    /**
     * Generic JSON 직렬화용 RedisTemplate
     * 타입 정보 포함 (클래스 역직렬화 가능)
     * Jackson 3는 default typing이 기본 OFF → 명시적으로 활성화
     */
    @Bean
    public RedisTemplate<String, Object> genericRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        GenericJacksonJsonRedisSerializer genericSerializer = GenericJacksonJsonRedisSerializer.builder()
                .enableDefaultTyping(redisTypeValidator())
                .enableSpringCacheNullValueSupport()
                .build();
        template.setValueSerializer(genericSerializer);
        template.setHashValueSerializer(genericSerializer);

        template.afterPropertiesSet();
        return template;
    }

    /**
     * 역직렬화 허용 타입 화이트리스트
     * (default typing 사용 시 임의 클래스 역직렬화 취약점 방지)
     */
    private PolymorphicTypeValidator redisTypeValidator() {
        return BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("io.miraclecat.fbs.")
                .allowIfSubType("java.util.")
                .allowIfSubType("java.time.")
                .allowIfSubType("java.math.")
                .allowIfSubType("java.lang.")
                .allowIfSubType("org.springframework.cache.support.NullValue")
                .build();
    }
}