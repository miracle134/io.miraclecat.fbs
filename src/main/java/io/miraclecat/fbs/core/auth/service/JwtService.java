package io.miraclecat.fbs.core.auth.service;

import com.google.gson.Gson;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jose.crypto.RSADecrypter;
import com.nimbusds.jose.crypto.RSAEncrypter;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import io.miraclecat.fbs.core.auth.config.JwtProperties;
import io.miraclecat.fbs.core.auth.dto.JwtDto;
import io.miraclecat.fbs.core.auth.dto.JwtExceptionCode;
import io.miraclecat.fbs.core.auth.dto.JwtValidationResult;
import io.miraclecat.fbs.core.auth.dto.MemberLoginDto;
import io.miraclecat.fbs.core.utils.crypto.EncryptUtil;
import io.miraclecat.fbs.core.utils.web.SessionAndCookieUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.util.Date;

/**
 * packageName    : io.miraclecat.fbs.auth
 * fileName       : JwtService
 * author         : MiracleCat 
 * date           : 2023-07-12
 * description    : JWT 생성
 */
@Slf4j
@Service
@Getter
public class JwtService {
    
    private final byte[] secretJwtKeyByte; // 키 Byte
    private final int accessExpiration; // access 단위(m)
    private final int refreshExpiration; // refresh 단위(m)
    private final long accessExpirationMs; // access 단위(ms)
    private final long refreshExpirationMs; // refresh 단위(ms)
    private final boolean cookieSecure; // 인증 쿠키 Secure (운영 HTTPS)
    
    public JwtService(JwtProperties jwtProperties, @Value("${app.cookie-secure:false}") boolean cookieSecure) {
        this.cookieSecure = cookieSecure;
        this.secretJwtKeyByte = EncryptUtil.base64DecodeToBytes(jwtProperties.getSecretJwtKey());
        this.accessExpiration = jwtProperties.getAccessExpiration();
        this.refreshExpiration = jwtProperties.getRefreshExpiration();
        this.accessExpirationMs = (long) jwtProperties.getAccessExpiration() * 60 * 1000;
        this.refreshExpirationMs = (long) jwtProperties.getRefreshExpiration() * 60 * 1000;
    }
    
    /**
     * Access JWT 생성
     */
    public String generateAccessToken(JwtDto jwtDto) {
        Gson gson = new Gson();
        String json = gson.toJson(jwtDto);
        return createSignedJWT(jwtDto.getLoginId(), json, accessExpirationMs);
    }

    /**
     * Refresh JWT 생성
     */
    public String generateRefreshToken(JwtDto jwtDto) {
        return createSignedJWT(jwtDto.getLoginId(), null, refreshExpirationMs);
    }
    
    /**
     * JWS 생성 (서명)
     */
    public String createSignedJWT(String loginId, String data, long expiration) {
        try {
            Date now = new Date();
            Date expiry = new Date(now.getTime() + expiration);
            JWSSigner signer = new MACSigner(secretJwtKeyByte);
            
            JWTClaimsSet.Builder claimsBuilder = new JWTClaimsSet.Builder()
                    .subject(loginId)
                    .claim("data", data)
                    .issueTime(now)
                    .expirationTime(expiry);
            if (data != null) {
                claimsBuilder.claim("data", data);
            }
        
            JWTClaimsSet claims = claimsBuilder.build();
            
            SignedJWT signedJWT = new SignedJWT(
                new JWSHeader(JWSAlgorithm.HS256),
                claims
            );
        
            signedJWT.sign(signer);
        
            return signedJWT.serialize();
        } catch (JOSEException e) {
            log.error("Error : Invalid JWT KEY ");
            throw new RuntimeException(e);
        }
    }

    
    /**
     * JWE 암호화
     */
    public String encryptAsJWE(String jwsCompact, PublicKey rsaPublicKey) throws Exception {
        JWEHeader header = new JWEHeader.Builder(
                JWEAlgorithm.RSA_OAEP_256,
                EncryptionMethod.A256GCM
        ).contentType("JWT")
         .build();
    
        Payload payload = new Payload(jwsCompact);
    
        JWEObject jweObject = new JWEObject(header, payload);
    
        RSAEncrypter encrypter = new RSAEncrypter((RSAPublicKey) rsaPublicKey);
        jweObject.encrypt(encrypter);
    
        return jweObject.serialize();
    }
    
    /**
     * JWE 복호화
     */
    public String decryptJWE(String jweCompact, PrivateKey rsaPrivateKey) throws Exception {
        JWEObject jweObject = JWEObject.parse(jweCompact);
        RSADecrypter decrypter = new RSADecrypter(rsaPrivateKey);
        jweObject.decrypt(decrypter);
    
        return jweObject.getPayload().toString();
    }
    
    /**
     * JWT 검증 및 만료일 체크
     */
    public JwtValidationResult validateJwt(String jwt) {
        try {
            // 파싱
            SignedJWT signedJWT = SignedJWT.parse(jwt);
    
            // 서명 검증
            JWSVerifier verifier = new MACVerifier(secretJwtKeyByte);
            if (!signedJWT.verify(verifier)) {
                log.warn("잘못된 JWT 서명입니다.");
                return new JwtValidationResult(JwtExceptionCode.INVALID_SIGNATURE, null);
            }
    
            // claims 꺼내기
            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
    
            // 만료일 확인
            Date expiration = claims.getExpirationTime();
            if (expiration != null && expiration.before(new Date())) {
                log.warn("만료된 JWT 토큰입니다. exp={}", expiration);
                return new JwtValidationResult(JwtExceptionCode.EXPIRED, claims);
            }
    
            // 성공
            return new JwtValidationResult(JwtExceptionCode.SUCCESS, claims);
    
        } catch (ParseException e) {
            log.warn("JWT 토큰 형식이 잘못되었습니다.", e);
            return new JwtValidationResult(JwtExceptionCode.MALFORMED, null);
        } catch (JOSEException e) {
            log.warn("JWT 서명 검증 실패.", e);
            return new JwtValidationResult(JwtExceptionCode.INVALID_SIGNATURE, null);
        } catch (Exception e) {
            log.error("JWT 처리 중 알 수 없는 오류.", e);
            return new JwtValidationResult(JwtExceptionCode.UNKNOWN, null);
        }
    }
    
    /**
     * JWT 검증 및 만료일 체크
     */
    public boolean isTokenExpired(String jws, String type) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(jws);
            JWSVerifier verifier = new MACVerifier(secretJwtKeyByte);
    
            if (!signedJWT.verify(verifier)) {
                log.error("Invalid signature. {}", type);
                return true;
            }
    
            Date expiration = signedJWT.getJWTClaimsSet().getExpirationTime();
            if (expiration == null) {
                log.error("No expiration claim. {}", type);
                return true;
            }
    
            return expiration.before(new Date());
        } catch (Exception e) {
            log.error("isTokenExpired : JWT 검증 실패. {}", type, e);
            return true;
        }
    }
    
    /**
     * 검증 후 claims 반환
     */
    public JWTClaimsSet getClaimsNimbus(String jws) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(jws);
            JWSVerifier verifier = new MACVerifier(secretJwtKeyByte);
    
            if (!signedJWT.verify(verifier)) {
                throw new SecurityException("Invalid signature!");
            }
    
            return signedJWT.getJWTClaimsSet();
        } catch (Exception e) {
            log.error("getClaims : JWT 검증 실패.", e);
            throw new RuntimeException("getClaims : Invalid JWT", e);
        }
    }
    
    /**
     * JWT 발행 (쿠키 저장)
     */
    @SneakyThrows
    public void jwtPublishWithCookie(HttpServletResponse response, JwtDto jwtDto) {
            String accessToken = generateAccessToken(jwtDto);
            String refreshToken = generateRefreshToken(jwtDto);
            
            String encryptAccessToken = encryptAsJWE(accessToken, EncryptUtil.getSystemRSAPublicKey());
            String encryptRefreshToken = encryptAsJWE(refreshToken, EncryptUtil.getSystemRSAPublicKey());
            
            SessionAndCookieUtil.setCookie(response
                    ,"Auth-Token-Access"
                    , "Bearer " + encryptAccessToken
                    , 60 * getRefreshExpiration()
                    , true
                    , cookieSecure);
            SessionAndCookieUtil.setCookie(response
                    ,"Auth-Token-Refresh"
                    , encryptRefreshToken
                    , 60 * getRefreshExpiration()
                    , true
                    , cookieSecure);
    }
    
    // JWT 데이터 설정
    public JwtDto convertLoginDtoToJwtDto(MemberLoginDto memberLoginDto) {
        return JwtDto.builder()
            .memberId(memberLoginDto.getMember().getMemberId())
            .loginId(memberLoginDto.getMember().getLoginId())
            .name(memberLoginDto.getMember().getMemberName())
            .role(memberLoginDto.getMember().getRole())
            .build();
    }

}
