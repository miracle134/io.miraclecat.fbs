package io.miraclecat.fbs.core.auth.filter;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jwt.JWTClaimsSet;
import io.miraclecat.fbs.api.member.domain.entity.Member;
import io.miraclecat.fbs.api.member.domain.repository.MemberRepository;
import io.miraclecat.fbs.core.auth.config.CustomAuthorityUtil;
import io.miraclecat.fbs.core.auth.dto.JwtDto;
import io.miraclecat.fbs.core.auth.dto.JwtExceptionCode;
import io.miraclecat.fbs.core.auth.dto.JwtValidationResult;
import io.miraclecat.fbs.core.auth.dto.MemberLoginDto;
import io.miraclecat.fbs.core.auth.service.JwtService;
import io.miraclecat.fbs.core.utils.common.StringUtil;
import io.miraclecat.fbs.core.utils.crypto.EncryptUtil;
import io.miraclecat.fbs.core.utils.web.SessionAndCookieUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * packageName    : io.miraclecat.fbs.auth.filter
 * fileName       : JwtVerificationFilter
 * author         : MiracleCat
 * date           : 2023-07-12
 * description    : JWT 검증
 */
@Slf4j
public class JwtVerificationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomAuthorityUtil customAuthorityUtil;
    private final MemberRepository memberRepository;
    
    public JwtVerificationFilter(JwtService jwtService, CustomAuthorityUtil customAuthorityUtil,
                                 MemberRepository memberRepository) {
        this.jwtService = jwtService;
        this.customAuthorityUtil = customAuthorityUtil;
        this.memberRepository = memberRepository;
    }
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        Gson gson = new Gson();

        try {
            String cookie = SessionAndCookieUtil.getCookie(request, "Auth-Token-Access");
            if (cookie == null || cookie.isEmpty()) {
                throw new Exception("Access Token 없음");
            }
            String accessJwe = SessionAndCookieUtil.getCookie(request, "Auth-Token-Access").replace("Bearer ", "");
            String accessJws = jwtService.decryptJWE(accessJwe, EncryptUtil.getSystemRSAPrivateKey());
            JwtValidationResult accessResult = jwtService.validateJwt(accessJws);
            switch (accessResult.getCode()) {
                case SUCCESS: {
                    JWTClaimsSet claims = accessResult.getClaims();
                    String json = (String) claims.getClaim("data");
                    JwtDto jwtDto = gson.fromJson(json, JwtDto.class);

                    // JWT 인증 정보 및 principal 담기
                    setAuthenticationToContext(jwtDto);
                    break;
                }
                case EXPIRED: {
                    log.info("Access Token 만료 - Refresh Token으로 재발급 시도");
                    // AccessToken은 만료되었지만 claims는 가져옴
                    JWTClaimsSet expiredClaims = accessResult.getClaims();
                    String json = (String) expiredClaims.getClaim("data");
                    JwtDto jwtDto = gson.fromJson(json, JwtDto.class);
                    
                    // 클라이언트가 보낸 Refresh Token 꺼내기
                    String refreshJwe = SessionAndCookieUtil.getCookie(request, "Auth-Token-Refresh");
                    String refreshJws = jwtService.decryptJWE(refreshJwe, EncryptUtil.getSystemRSAPrivateKey());
                    JwtValidationResult refreshResult = jwtService.validateJwt(refreshJws);
                    
                    // Refresh Token 검증
                    if (refreshResult.getCode() == JwtExceptionCode.SUCCESS) {
                        // 재발급 시 회원 상태 확인 (정지·탈퇴 차단, 권한·이름은 최신값으로)
                        Member member = memberRepository.findByLoginIdAndWithdrawnAt(jwtDto.getLoginId())
                                .filter(m -> !m.isSuspended())
                                .orElse(null);
                        if (member == null) {
                            SessionAndCookieUtil.removeCookie(response, "Auth-Token-Access");
                            SessionAndCookieUtil.removeCookie(response, "Auth-Token-Refresh");
                            throw new Exception("이용할 수 없는 계정 - 재로그인 요구");
                        }
                        JwtDto renewed = jwtService.convertLoginDtoToJwtDto(new MemberLoginDto(member, customAuthorityUtil));

                        jwtService.jwtPublishWithCookie(response, renewed);
                        log.info("Token 재발급 완료");

                        // JWT 인증 정보 및 principal 담기
                        setAuthenticationToContext(renewed);
                    } else {
                        log.warn("Refresh Token이 유효하지 않음 - 재로그인 요구");
                        throw new Exception("Refresh Token이 유효하지 않음 - 재로그인 요구");
                    }
                    break;
                }
                case INVALID_SIGNATURE:
                    throw new Exception("INVALID_SIGNATURE");
                case MALFORMED:
                    throw new Exception("MALFORMED");
                case UNKNOWN:
                default:
                    log.warn("Access Token이 유효하지 않음 - 요청 거부");
                    throw new Exception("Access Token이 유효하지 않음 - 요청 거부");
            }
        
        } catch (JOSEException | JsonSyntaxException e) {
            log.warn("JWT 처리 실패: {}", e.getMessage());
            request.setAttribute("exception", e);
        } catch (Exception e) {
            log.error("예상치 못한 예외: {}", e.getMessage(), e);
            request.setAttribute("exception", e);
        }


        filterChain.doFilter(request, response);
    }

    @SneakyThrows
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String authorization = SessionAndCookieUtil.getCookie(request, "Auth-Token-Access");

        return StringUtil.isEmptyOrWhitespace(authorization);
    }

    // JWT 인증 정보 및 principal 담기
    private void setAuthenticationToContext(JwtDto jwtDto) {
        Authentication authentication = new UsernamePasswordAuthenticationToken(jwtDto, null, customAuthorityUtil.createAuthorities(jwtDto.getRole()));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
    
}
