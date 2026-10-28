package io.miraclecat.fbs.web.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

/**
 * packageName    : io.miraclecat.fbs.web.controller
 * fileName       : MemberViewController
 * author         : MiracleCat
 * date           : 2026-09-21
 * description    :
 */

@Slf4j
@Controller
public class MemberViewController {

	/**
	 * 회원가입
	 */
    @GetMapping("/member/signup")
	public ModelAndView memberSignupPage() {
		ModelAndView mv = new ModelAndView("/page/member/auth/signup");
		return mv;
	}

}