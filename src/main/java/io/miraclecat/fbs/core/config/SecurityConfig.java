package io.miraclecat.fbs.core.config;

import io.miraclecat.fbs.api.member.domain.repository.MemberRepository;
import io.miraclecat.fbs.core.audit.authentication.service.AuthenticationLogService;
import io.miraclecat.fbs.core.auth.config.CustomAuthorityUtil;
import io.miraclecat.fbs.core.auth.config.RedisOAuth2AuthorizationRequestRepository;
import io.miraclecat.fbs.core.auth.filter.JwtAuthenticationFilter;
import io.miraclecat.fbs.core.auth.filter.JwtVerificationFilter;
import io.miraclecat.fbs.core.auth.handler.AuthDeniedHandler;
import io.miraclecat.fbs.core.auth.handler.AuthEntryPoint;
import io.miraclecat.fbs.core.auth.handler.ExternalApiDeniedHandler;
import io.miraclecat.fbs.core.auth.handler.ExternalApiEntryPointHandler;
import io.miraclecat.fbs.core.auth.handler.OAuth2FailureHandler;
import io.miraclecat.fbs.core.auth.handler.OAuth2SuccessHandler;
import io.miraclecat.fbs.core.auth.service.JwtService;
import jakarta.servlet.DispatcherType;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.security.autoconfigure.web.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * packageName    : io.miraclecat.fbs.core.config
 * fileName       : SecurityConfig
 * author         : MiracleCat
 * date           : 2023-07-07
 * description    : Spring Security (전 프로파일 공통, 디버그만 spring.security.debug 로 제어)
 *                  - STATELESS + JWE 쿠키, Google OAuth2 (인가 요청은 Redis 저장)
 *                  - 관리자: /admin/**, /api/admin/** → ROLE_ADMIN, ROLE_SUPER_ADMIN
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    @Value("${spring.security.debug:false}")
    private boolean securityDebug;

    private final JwtService jwtService;
    private final CustomAuthorityUtil customAuthorityUtil;
    private final AuthenticationConfiguration authenticationConfiguration;
    private final AuthEntryPoint authEntryPoint;
    private final AuthDeniedHandler authDeniedHandler;
    private final ExternalApiEntryPointHandler externalApiEntryPointHandler;
    private final ExternalApiDeniedHandler externalApiDeniedHandler;
    private final AuthenticationLogService authenticationLogService;
    private final RedisOAuth2AuthorizationRequestRepository oAuth2AuthorizationRequestRepository;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final OAuth2FailureHandler oAuth2FailureHandler;
    private final MemberRepository memberRepository;

    /**
     * 1. 외부 제공 API (/ext/**)
     */
    @Bean
    @Order(1)
    public SecurityFilterChain externalApiFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/ext/**")
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(externalApiEntryPointHandler)
                        .accessDeniedHandler(externalApiDeniedHandler)
                )
                .authorizeHttpRequests(request -> request
                        .anyRequest().authenticated()
                );
        return http.build();
    }

    /**
     * 2. 화면 / API
     */
    @Bean
    @Order(2)
    public SecurityFilterChain mainFilterChain(HttpSecurity http) throws Exception {
        AuthenticationManager authenticationManager = authenticationConfiguration.getAuthenticationManager();
        JwtAuthenticationFilter jwtAuthenticationFilter =
                new JwtAuthenticationFilter(authenticationManager, jwtService, authenticationLogService);
        JwtVerificationFilter jwtVerificationFilter =
                new JwtVerificationFilter(jwtService, customAuthorityUtil, memberRepository);

        http
                // CSRF: XSRF-TOKEN 쿠키 → axios가 X-XSRF-TOKEN 헤더로 전송 (GET 등 안전한 메서드 제외)
                .csrf(csrf -> csrf.spa())
                .cors(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authEntryPoint)
                        .accessDeniedHandler(authDeniedHandler)
                )
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/auth/login")
                        .authorizationEndpoint(endpoint -> endpoint
                                .authorizationRequestRepository(oAuth2AuthorizationRequestRepository))
                        .successHandler(oAuth2SuccessHandler)
                        .failureHandler(oAuth2FailureHandler)
                )
                .authorizeHttpRequests(request -> request
                        .dispatcherTypeMatchers(DispatcherType.FORWARD, DispatcherType.ERROR).permitAll()
                        .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()
                        .requestMatchers("/admin/login").permitAll()
                        .requestMatchers("/admin", "/admin/**", "/api/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                        .anyRequest().permitAll()
                )
                .addFilterBefore(jwtVerificationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAt(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return web -> web.debug(securityDebug);
    }

    /**
     * 비밀번호 암호화: BCrypt
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
