package io.miraclecat.fbs.core.auth;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * packageName    : io.miraclecat.fbs.core.auth
 * fileName       : AuthRole
 * author         : MiracleCat
 * date           : 2025-04-21
 * description    :
 */
@RequiredArgsConstructor
@Getter
public enum AuthRole {

    // 통합 사용자 Role
    ROLE_SUPER_ADMIN("ROLE_SUPER_ADMIN", "통합관리자", "개발사 최고 관리자"),
    ROLE_ADMIN("ROLE_ADMIN", "관리자", "시스템 관리자"),
    ROLE_USER("ROLE_USER", "일반사용자", "일반 사용자"),

    // 외부 Open API Role
    ROLE_EXTERNAL_ADMIN("ROLE_EXTERNAL_ADMIN", "외부API최고관리자", "외부 API 최고 관리자"),
    ROLE_EXTERNAL_MANAGER("ROLE_EXTERNAL_MANAGER", "외부API일반관리자", "외부 API 일반 관리자");

    private final String role;
    private final String roleName;
    private final String description;

    @JsonCreator
    public static AuthRole from(String role) {
        return Arrays.stream(AuthRole.values())
            .filter(r -> r.getRole().equals(role))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown role: " + role));
    }

    @JsonValue
    public String getRole() {
        return role;
    }

}
