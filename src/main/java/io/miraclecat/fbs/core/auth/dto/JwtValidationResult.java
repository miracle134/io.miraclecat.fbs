package io.miraclecat.fbs.core.auth.dto;

import com.nimbusds.jwt.JWTClaimsSet;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * packageName    : io.miraclecat.fbs.core.auth.dto
 * fileName       : JwtValidationResult
 * author         : MiracleCat 
 * date           : 2025-07-16
 * description    :
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class JwtValidationResult {
	private JwtExceptionCode code;
	private JWTClaimsSet claims;
}
