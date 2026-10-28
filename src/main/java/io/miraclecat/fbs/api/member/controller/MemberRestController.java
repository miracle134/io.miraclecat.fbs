package io.miraclecat.fbs.api.member.controller;

import io.miraclecat.fbs.api.member.dto.MemberAccountDto;
import io.miraclecat.fbs.api.member.dto.MemberAdminDto;
import io.miraclecat.fbs.api.member.service.MemberAccountService;
import io.miraclecat.fbs.api.member.service.MemberAdminService;
import io.miraclecat.fbs.core.response.ApiEntity;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * packageName    : io.miraclecat.fbs.member.controller
 * fileName       : MemberController
 * author         : MiracleCat 
 * date           : 2025-04-11
 * description    :
 */
@RestController
@RequiredArgsConstructor
public class MemberRestController {

	private final MemberAccountService memberAccountService;
	private final MemberAdminService memberAdminService;

	/**
	 * 회원가입
	 */
	@PostMapping("/api/member")
	public ApiEntity<?> createMember(@RequestBody MemberAccountDto.SignupReq req) {
		return memberAccountService.signup(req);
	}

	/**
	 * 내 정보
	 */
	@GetMapping("/api/member/me")
	public ApiEntity<?> getMe() {
		return memberAccountService.getMe();
	}

	/**
	 * 이름 / 전화번호 수정
	 */
	@PutMapping("/api/member/me")
	public ApiEntity<?> updateProfile(@RequestBody MemberAccountDto.ProfileReq req, HttpServletResponse response) {
		return memberAccountService.updateProfile(req, response);
	}

	/**
	 * 비밀번호 변경
	 */
	@PutMapping("/api/member/me/password")
	public ApiEntity<?> changePassword(@RequestBody MemberAccountDto.PasswordReq req) {
		return memberAccountService.changePassword(req);
	}

	/**
	 * 회원 목록 (관리자)
	 */
	@GetMapping("/api/admin/member")
	public ApiEntity<?> getAdminMembers(@ModelAttribute MemberAdminDto.SearchReq req) {
		return memberAdminService.getMembers(req);
	}

	/**
	 * 회원 상세 (관리자)
	 */
	@GetMapping("/api/admin/member/{member-id}")
	public ApiEntity<?> getAdminMemberDetail(@PathVariable("member-id") Long memberId) {
		return memberAdminService.getMemberDetail(memberId);
	}

	/**
	 * 회원 정지 / 해제 (관리자)
	 */
	@PutMapping("/api/admin/member/{member-id}/status")
	public ApiEntity<?> changeMemberStatus(@PathVariable("member-id") Long memberId,
										   @RequestBody MemberAdminDto.StatusReq req) {
		return memberAdminService.changeStatus(memberId, req);
	}

}
