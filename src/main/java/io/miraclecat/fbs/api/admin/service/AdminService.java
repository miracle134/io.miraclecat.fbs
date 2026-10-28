/**
 * packageName    : io.miraclecat.fbs.api.admin.service
 * fileName       : AdminService
 * author         : MiracleCat
 * date           : 2026-05-19
 * description    : Admin 서비스 (설정, 취소정책, 스케줄러 로그)
 */
package io.miraclecat.fbs.api.admin.service;

import io.miraclecat.fbs.api.admin.dao.AdminDao;
import io.miraclecat.fbs.api.admin.domain.entity.Setting;
import io.miraclecat.fbs.api.admin.domain.repository.SettingRepository;
import io.miraclecat.fbs.api.admin.dto.SchedulerLogDto;
import io.miraclecat.fbs.api.admin.dto.SettingDto;
import io.miraclecat.fbs.core.config.ReadTx;
import io.miraclecat.fbs.core.config.RequiredTx;
import io.miraclecat.fbs.core.response.ApiEntity;
import io.miraclecat.fbs.core.response.HttpsStatusCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

import static io.miraclecat.fbs.core.exception.GlobalException.business;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final SettingRepository settingRepository;
    private final AdminDao adminDao;

    // ==================== 설정 ====================

    /**
     * 설정 목록 조회
     */
    @ReadTx
    public ApiEntity<List<SettingDto.SearchRes>> getSettings(SettingDto.SearchReq req) {
        List<SettingDto.SearchRes> settings = adminDao.findSettings(req);
        return ApiEntity.of(HttpsStatusCode.SUCCESS, settings);
    }

    /**
     * 설정 상세 조회
     */
    @ReadTx    
    public ApiEntity<SettingDto.DetailRes> getSettingDetail(Long settingId) {
        SettingDto.DetailRes setting = adminDao.findSettingDetail(settingId);
        return ApiEntity.of(HttpsStatusCode.SUCCESS, setting);
    }

    /**
     * 설정 등록
     */
    @RequiredTx
    public ApiEntity<Void> createSetting(SettingDto.CreateReq req) {
        Setting setting = Setting.builder()
                .settingKey(req.getSettingKey())
                .settingValue(req.getSettingValue())
                .description(req.getDescription())
                .build();
        settingRepository.save(setting);
        return ApiEntity.of(HttpsStatusCode.REGISTER);
    }

    /**
     * 설정 수정
     */
    @RequiredTx
    public ApiEntity<Void> updateSetting(Long settingId, SettingDto.UpdateReq req, Long updatedId) {
        Setting setting = settingRepository.findById(settingId)
                .orElseThrow(() -> business(HttpsStatusCode.RESOURCE_NOT_FOUND));
        setting.updateSetting(req, updatedId);
        return ApiEntity.of(HttpsStatusCode.SUCCESS);
    }

    /**
     * 설정 삭제
     */
    @RequiredTx
    public ApiEntity<Void> deleteSetting(Long settingId) {
        settingRepository.findById(settingId)
                .orElseThrow(() -> business(HttpsStatusCode.RESOURCE_NOT_FOUND));
        settingRepository.deleteById(settingId);
        return ApiEntity.of(HttpsStatusCode.SUCCESS);
    }

    // ==================== 스케줄러 로그 ====================

    /**
     * 스케줄러 로그 목록 조회
     */
    @ReadTx
    public ApiEntity<List<SchedulerLogDto.SearchRes>> getSchedulerLogs(SchedulerLogDto.SearchReq req) {
        List<SchedulerLogDto.SearchRes> logs = adminDao.findSchedulerLogs(req);
        return ApiEntity.of(HttpsStatusCode.SUCCESS, logs);
    }

    /**
     * 스케줄러 로그 상세 조회
     */
    @ReadTx
    public ApiEntity<SchedulerLogDto.DetailRes> getSchedulerLogDetail(Long schedulerLogId) {
        SchedulerLogDto.DetailRes log = adminDao.findSchedulerLogDetail(schedulerLogId);
        return ApiEntity.of(HttpsStatusCode.SUCCESS, log);
    }

}
