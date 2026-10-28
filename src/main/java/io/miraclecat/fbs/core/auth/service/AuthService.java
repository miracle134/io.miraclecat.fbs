package io.miraclecat.fbs.core.auth.service;

import io.miraclecat.fbs.api.member.domain.entity.Member;
import io.miraclecat.fbs.api.member.domain.repository.MemberRepository;
import io.miraclecat.fbs.core.auth.config.CustomAuthorityUtil;
import io.miraclecat.fbs.core.auth.dto.MemberLoginDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * packageName    : io.miraclecat.fbs.auth.service
 * fileName       : AuthService
 * author         : MiracleCat
 * date           : 2023-07-07
 * description    :
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService implements UserDetailsService {

	private final MemberRepository memberRepository;
	private final CustomAuthorityUtil customAuthorityUtil;

	/**
	 * 로그인 수행
	 */
	public UserDetails loadUserByUsername(String loginId) throws UsernameNotFoundException {
        Member member = memberRepository
                .findByLoginIdAndWithdrawnAt(loginId)
                .orElseThrow(() -> new UsernameNotFoundException("회원 정보가 없습니다."));

        return new MemberLoginDto(member, customAuthorityUtil);
	}

}

