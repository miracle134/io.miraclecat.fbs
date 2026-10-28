package io.miraclecat.fbs.core.utils.security;

import io.miraclecat.fbs.core.auth.dto.JwtDto;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * packageName    : io.miraclecat.fbs.core.utils.security
 * fileName       : SecurityUtil
 * author         : MiracleCat
 * date           : 25. 9. 15.
 * description    :
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SecurityUtil {

    public static JwtDto getCurrentMember() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof JwtDto jwtDto) {
            return jwtDto;
        }
        return null;
    }

    public static Long getCurrentMemberId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof JwtDto jwtDto) {
            return jwtDto.getMemberId();
        }
        return null;
    }

    public static String getCurrentMemberName() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof JwtDto jwtDto) {
            return jwtDto.getName();
        }
        return null;
    }

}