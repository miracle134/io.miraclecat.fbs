/**
 * packageName    : io.miraclecat.fbs.api.admin.controller
 * fileName       : AdminRestController
 * author         : MiracleCat
 * date           : 2026-05-19
 * description    : Admin 컨트롤러 (설정, 스케줄러 로그)
 */
package io.miraclecat.fbs.api.admin.controller;

import io.miraclecat.fbs.api.admin.dto.SchedulerLogDto;
import io.miraclecat.fbs.api.admin.dto.SettingDto;
import io.miraclecat.fbs.api.admin.service.AdminService;
import io.miraclecat.fbs.core.response.ApiEntity;
import io.miraclecat.fbs.core.utils.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class AdminRestController {

    private final AdminService adminService;

    // ==================== 설정 ====================

    /**
     * 설정 목록 조회
     * @param req req
     * @return ApiEntity
     */
    @GetMapping("/api/admin/setting")
    public ApiEntity<?> getSettings(@ModelAttribute SettingDto.SearchReq req) {
        return adminService.getSettings(req);
    }

    /**
     * 설정 상세 조회
     * @param settingId settingId
     * @return ApiEntity
     */
    @GetMapping("/api/admin/setting/{setting-id}")
    public ApiEntity<?> getSettingDetail(@PathVariable("setting-id") Long settingId) {
        return adminService.getSettingDetail(settingId);
    }

    /**
     * 설정 등록
     * @param req req
     * @return ApiEntity
     */
    @PostMapping("/api/admin/setting")
    public ApiEntity<?> createSetting(@RequestBody SettingDto.CreateReq req) {
        return adminService.createSetting(req);
    }

    /**
     * 설정 수정
     * @param settingId settingId
     * @param req req
     * @return ApiEntity
     */
    @PutMapping("/api/admin/setting/{setting-id}")
    public ApiEntity<?> updateSetting(@PathVariable("setting-id") Long settingId,
                                      @RequestBody SettingDto.UpdateReq req) {
        return adminService.updateSetting(settingId, req, SecurityUtil.getCurrentMemberId());
    }

    /**
     * 설정 삭제
     * @param settingId settingId
     * @return ApiEntity
     */
    @DeleteMapping("/api/admin/setting/{setting-id}")
    public ApiEntity<?> deleteSetting(@PathVariable("setting-id") Long settingId) {
        return adminService.deleteSetting(settingId);
    }

    // ==================== 스케줄러 로그 ====================

    /**
     * 스케줄러 로그 목록 조회
     * @param req req
     * @return ApiEntity
     */
    @GetMapping("/api/admin/scheduler-log")
    public ApiEntity<?> getSchedulerLogs(@ModelAttribute SchedulerLogDto.SearchReq req) {
        return adminService.getSchedulerLogs(req);
    }

    /**
     * 스케줄러 로그 상세 조회
     * @param schedulerLogId schedulerLogId
     * @return ApiEntity
     */
    @GetMapping("/api/admin/scheduler-log/{schedulerLog-id}")
    public ApiEntity<?> getSchedulerLogDetail(@PathVariable("schedulerLog-id") Long schedulerLogId) {
        return adminService.getSchedulerLogDetail(schedulerLogId);
    }

}
