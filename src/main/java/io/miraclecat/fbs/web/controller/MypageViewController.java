package io.miraclecat.fbs.web.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

/**
 * packageName    : io.miraclecat.fbs.web.controller
 * fileName       : MypageViewController
 * author         : MiracleCat
 * date           : 2026-10-25
 * description    :
 */
@Slf4j
@Controller
public class MypageViewController {

    /**
     * 회원가입
     */
    @GetMapping("/mypage")
    public ModelAndView memberSignupPage() {
        ModelAndView mv = new ModelAndView("/page/member/mypage/mypage");
        return mv;
    }

}