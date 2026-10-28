package io.miraclecat.fbs.core.auth.config;

import io.miraclecat.fbs.core.utils.web.RedirectUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.stereotype.Component;

import java.io.*;
import java.time.Duration;
import java.util.Base64;

/**
 * packageName    : io.miraclecat.fbs.core.auth.config
 * fileName       : RedisOAuth2AuthorizationRequestRepository
 * author         : MiracleCat
 * date           : 2026-10-26
 * description    : OAuth2 인가 요청 저장소 (세션 대신 Redis, key = state)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisOAuth2AuthorizationRequestRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    private static final String REQUEST_KEY = "oauth2:auth-request:";
    private static final String REDIRECT_KEY = "oauth2:redirect:";
    private static final Duration TTL = Duration.ofMinutes(5);

    private final StringRedisTemplate redisTemplate;

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        String state = request.getParameter(OAuth2ParameterNames.STATE);
        if (state == null) return null;
        String value = redisTemplate.opsForValue().get(REQUEST_KEY + state);
        return value == null ? null : deserialize(value);
    }

    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest,
                                         HttpServletRequest request, HttpServletResponse response) {
        if (authorizationRequest == null) return;
        String state = authorizationRequest.getState();
        redisTemplate.opsForValue().set(REQUEST_KEY + state, serialize(authorizationRequest), TTL);

        // 로그인 후 돌아갈 경로
        String redirect = request.getParameter("redirect");
        if (RedirectUtil.isSafe(redirect)) {
            redisTemplate.opsForValue().set(REDIRECT_KEY + state, redirect, TTL);
        }
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request,
                                                                 HttpServletResponse response) {
        String state = request.getParameter(OAuth2ParameterNames.STATE);
        if (state == null) return null;
        String value = redisTemplate.opsForValue().getAndDelete(REQUEST_KEY + state);
        return value == null ? null : deserialize(value);
    }

    /**
     * 로그인 후 이동 경로 꺼내기 (1회용)
     */
    public String popRedirect(HttpServletRequest request) {
        String state = request.getParameter(OAuth2ParameterNames.STATE);
        if (state == null) return null;
        return redisTemplate.opsForValue().getAndDelete(REDIRECT_KEY + state);
    }

    private String serialize(OAuth2AuthorizationRequest req) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(req);
            oos.flush();
            return Base64.getEncoder().encodeToString(bos.toByteArray());
        } catch (IOException e) {
            throw new IllegalStateException("OAuth2 인가 요청 직렬화 실패", e);
        }
    }

    private OAuth2AuthorizationRequest deserialize(String value) {
        try (ObjectInputStream ois = new ObjectInputStream(
                new ByteArrayInputStream(Base64.getDecoder().decode(value)))) {
            return (OAuth2AuthorizationRequest) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            log.warn("OAuth2 인가 요청 역직렬화 실패", e);
            return null;
        }
    }
}
