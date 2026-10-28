/**
 * packageName    : static.js.domain
 * fileName       : admin-api
 * date           : 2026-05-19
 * description    : Admin API (설정, 취소정책, 스케줄러 로그)
 */
// Utils
import {ajax} from '@core/core.js';

const BASE_URL = '/api/admin';

const adminApi = {

    // ==================== 설정 ====================

    /**
     * 설정 목록 조회
     * GET /api/admin/setting
     */
    async getSettings(params = {}, isLoading) {
        return await ajax.getAjax(params, `${BASE_URL}/setting`, isLoading);
    },

    /**
     * 설정 상세 조회
     * GET /api/admin/setting/${setting-id}
     */
    async getSettingDetail(id, isLoading) {
        return await ajax.getAjax({}, `${BASE_URL}/setting/${id}`, isLoading);
    },

    /**
     * 설정 등록
     * POST /api/admin/setting
     */
    async createSetting(data, isLoading) {
        return await ajax.postAjax(data, `${BASE_URL}/setting`, isLoading);
    },

    /**
     * 설정 수정
     * PUT /api/admin/setting/${setting-id}
     */
    async updateSetting(id, data, isLoading) {
        return await ajax.putAjax(data, `${BASE_URL}/setting/${id}`, isLoading);
    },

    /**
     * 설정 삭제
     * DELETE /api/admin/setting/${setting-id}
     */
    async deleteSetting(id, isLoading) {
        return await ajax.deleteAjax({}, `${BASE_URL}/setting/${id}`, isLoading);
    },

    // ==================== 스케줄러 로그 ====================

    /**
     * 스케줄러 로그 목록 조회
     * GET /api/admin/scheduler-log
     */
    async getSchedulerLogs(params = {}, isLoading) {
        return await ajax.getAjax(params, `${BASE_URL}/scheduler-log`, isLoading);
    },

    /**
     * 스케줄러 로그 상세 조회
     * GET /api/admin/scheduler-log/${schedulerLog-id}
     */
    async getSchedulerLogDetail(id, isLoading) {
        return await ajax.getAjax({}, `${BASE_URL}/scheduler-log/${id}`, isLoading);
    },

    // ==================== 대시보드 / 통계 ====================

    /**
     * 대시보드 요약
     * GET /api/admin/stats/dashboard
     */
    async getDashboardStats(isLoading) {
        return await ajax.getAjax({}, `${BASE_URL}/stats/dashboard`, isLoading);
    },

    /**
     * 기간 통계 (startDate, endDate: yyyy-MM-dd)
     * GET /api/admin/stats
     */
    async getPeriodStats(params = {}, isLoading) {
        return await ajax.getAjax(params, `${BASE_URL}/stats`, isLoading);
    },

};

export default adminApi;
