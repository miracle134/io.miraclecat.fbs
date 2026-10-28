package io.miraclecat.fbs.core.auth.dto;

import io.miraclecat.fbs.api.member.domain.entity.Member;
import io.miraclecat.fbs.core.auth.config.CustomAuthorityUtil;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

/**
 * packageName    : io.miraclecat.fbs.auth.dto
 * fileName       : MemberJwtDto
 * author         : MiracleCat
 * date           : 2023-07-14
 * description    :
 */
@Getter
@Setter
public class MemberLoginDto implements UserDetails {

    private Member member;
    private CustomAuthorityUtil customAuthorityUtil;


    public MemberLoginDto(Member member, CustomAuthorityUtil customAuthorityUtil) {
        this.member = member;
        this.customAuthorityUtil = customAuthorityUtil;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return customAuthorityUtil.createAuthorities(member.getRole());
    }

    @Override
    public String getPassword() {
        return member.getLoginPassword();
    }

    @Override
    public String getUsername() {
        return member.getLoginId();
    }

    @Override
    public boolean isAccountNonExpired() {
	    return UserDetails.super.isAccountNonExpired();
    }

    @Override
    public boolean isAccountNonLocked() {
	    return UserDetails.super.isAccountNonLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
	    return UserDetails.super.isCredentialsNonExpired();
    }

    @Override
    public boolean isEnabled() {
	    return UserDetails.super.isEnabled();
    }

}
