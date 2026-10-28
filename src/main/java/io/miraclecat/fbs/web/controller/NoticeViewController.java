package io.miraclecat.fbs.web.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.ModelAndView;

/**
 * packageName    : io.miraclecat.fbs.web.controller
 * fileName       : NoticeViewController
 * author         : MiracleCat
 * date           : 2026-10-27
 * description    : 공지사항 화면 (사용자)
 */
@Slf4j
@Controller
public class NoticeViewController {

    /**
     * 공지사항 목록
     */
    @GetMapping("/notice")
    public ModelAndView noticeListPage() {
        return new ModelAndView("/page/member/notice/notice-list");
    }

    /**
     * 공지사항 상세
     */
    @GetMapping("/notice/{notice-id}")
    public ModelAndView noticeDetailPage(@PathVariable("notice-id") Long noticeId) {
        ModelAndView mv = new ModelAndView("/page/member/notice/notice-detail");
        mv.addObject("noticeId", noticeId);
        return mv;
    }

}
