package io.miraclecat.fbs.core.auth.config;

import io.miraclecat.fbs.core.auth.AuthRole;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * packageName    : io.miraclecat.fbs.auth.utils
 * fileName       : CustomAuthorityUtils
 * author         : MiracleCat 
 * date           : 2023-07-13
 * description    :
 */
@Slf4j
@Component
public class CustomAuthorityUtil {

    public List<GrantedAuthority> createAuthorities(AuthRole role) {
        return AuthorityUtils.createAuthorityList(role.getRole());
    }

}
