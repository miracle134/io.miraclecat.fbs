package io.miraclecat.fbs.core.config;

import io.miraclecat.fbs.core.auth.config.CustomAuthorityUtil;
import io.miraclecat.fbs.core.auth.service.JwtService;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;

/**
 * packageName    : io.miraclecat.fbs.config
 * fileName       : CustomAuthorityConfig
 * author         : MiracleCat
 * date           : 2023-07-13
 * description    : Custom JWT 인증 Config
 */
public class CustomAuthorityConfig extends AbstractHttpConfigurer<CustomAuthorityConfig, HttpSecurity> {

    private final JwtService jwtService;
    private final CustomAuthorityUtil customAuthorityUtil; // 추가

    public CustomAuthorityConfig(JwtService jwtService, CustomAuthorityUtil customAuthorityUtil) {
        this.jwtService = jwtService;
        this.customAuthorityUtil = customAuthorityUtil;
    }

    @Override
    public void configure(HttpSecurity builder) {
//        AuthenticationManager authenticationManager = builder.getSharedObject(AuthenticationManager.class);
//
//        JwtAuthenticationFilter jwtAuthenticationFilter = new JwtAuthenticationFilter(authenticationManager, jwtService);
//        jwtAuthenticationFilter.setFilterProcessesUrl("/auth/loginProcess");
//        jwtAuthenticationFilter.setAuthenticationSuccessHandler(new AuthSuccessHandler());
//        jwtAuthenticationFilter.setAuthenticationFailureHandler(new AuthFailureHandler());
//
//        JwtVerificationFilter jwtVerificationFilter = new JwtVerificationFilter(jwtService, customAuthorityUtil);
//
//        builder.addFilter(jwtAuthenticationFilter)
//            .addFilterAfter(jwtVerificationFilter, JwtAuthenticationFilter.class);
    }
}
