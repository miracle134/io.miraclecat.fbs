package io.miraclecat.fbs.web.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import java.util.Set;

/**
 * packageName    : io.miraclecat.fbs.web.controller
 * fileName       : AdminViewController
 * author         : MiracleCat
 * date           : 2026-10-27
 * description    : 관리자 화면 (/admin/login 제외 ROLE_ADMIN·ROLE_SUPER_ADMIN 전용 — SecurityConfig)
 *                  pageTitle: 상단 제목 / menu: 사이드바 활성 메뉴
 */
@Slf4j
@Controller
public class AdminViewController {

    private static final Set<String> ADMIN_ROLES = Set.of("ROLE_ADMIN", "ROLE_SUPER_ADMIN");

    /**
     * 관리자 로그인 (이미 관리자로 로그인 상태면 대시보드로)
     */
    @GetMapping("/admin/login")
    public ModelAndView loginPage(Authentication authentication) {
        if (isAdmin(authentication)) {
            return new ModelAndView("redirect:/admin/dashboard");
        }
        ModelAndView mv = new ModelAndView("/page/admin/auth/login");
        mv.addObject("pageTitle", "관리자 로그인");
        return mv;
    }

    @GetMapping("/admin")
    public String index() {
        return "redirect:/admin/dashboard";
    }

    /**
     * 대시보드
     */
    @GetMapping("/admin/dashboard")
    public ModelAndView dashboardPage() {
        return page("/page/admin/dashboard/dashboard", "대시보드", "dashboard");
    }

    /**
     * 항공편 관리
     */
    @GetMapping("/admin/flights")
    public ModelAndView flightsPage() {
        return page("/page/admin/flight/flights", "항공편 관리", "flights");
    }

    /**
     * 예약 관리
     */
    @GetMapping("/admin/bookings")
    public ModelAndView bookingsPage() {
        return page("/page/admin/booking/bookings", "예약 관리", "bookings");
    }

    /**
     * 회원 관리
     */
    @GetMapping("/admin/users")
    public ModelAndView usersPage() {
        return page("/page/admin/member/users", "회원 관리", "users");
    }

    /**
     * 공지사항 관리
     */
    @GetMapping("/admin/notices")
    public ModelAndView noticesPage() {
        return page("/page/admin/notice/notices", "공지사항 관리", "notices");
    }

    /**
     * 통계
     */
    @GetMapping("/admin/statistics")
    public ModelAndView statisticsPage() {
        return page("/page/admin/statistics/statistics", "통계", "statistics");
    }

    private ModelAndView page(String view, String pageTitle, String menu) {
        ModelAndView mv = new ModelAndView(view);
        mv.addObject("pageTitle", pageTitle);
        mv.addObject("menu", menu);
        return mv;
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> ADMIN_ROLES.contains(a.getAuthority()));
    }

}
