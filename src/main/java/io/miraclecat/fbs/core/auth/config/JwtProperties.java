package io.miraclecat.fbs.core.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * packageName    : io.miraclecat.fbs.core.auth.config
 * fileName       : JwtProperties
 * author         : MiracleCat 
 * date           : 2025-07-10
 * description    :
 */
@Component
@ConfigurationProperties(prefix = "jwt")
@Getter
@Setter
public class JwtProperties {
	private String secretJwtKey;
	private int accessExpiration;
	private int refreshExpiration;
}

