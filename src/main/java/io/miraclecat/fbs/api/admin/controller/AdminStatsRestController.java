package io.miraclecat.fbs.api.admin.controller;

import io.miraclecat.fbs.api.admin.dto.StatsDto;
import io.miraclecat.fbs.api.admin.service.AdminStatsService;
import io.miraclecat.fbs.core.response.ApiEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RestController;

/**
 * packageName    : io.miraclecat.fbs.api.admin.controller
 * fileName       : AdminStatsRestController
 * author         : MiracleCat
 * date           : 2026-10-27
 * description    : 관리자 대시보드 / 통계 (/api/admin/** — 관리자 권한)
 */
@RestController
@RequiredArgsConstructor
public class AdminStatsRestController {

    private final AdminStatsService adminStatsService;

    /**
     * 대시보드 요약
     */
    @GetMapping("/api/admin/stats/dashboard")
    public ApiEntity<?> getDashboard() {
        return adminStatsService.getDashboard();
    }

    /**
     * 기간 통계 (startDate ~ endDate, 기본 최근 30일)
     */
    @GetMapping("/api/admin/stats")
    public ApiEntity<?> getPeriodStats(@ModelAttribute StatsDto.PeriodReq req) {
        return adminStatsService.getPeriodStats(req);
    }
}
