/**
 * packageName    : static.js.domain
 * fileName       : notice-api
 * date           : 2026-05-19
 * description    : 공지사항 API (사용자 /api/notice, 관리자 /api/admin/notice)
 */
// Utils
import {ajax} from '@core/core.js';

const BASE_URL = '/api/notice';
const ADMIN_URL = '/api/admin/notice';

const noticeApi = {

    /**
     * 공지사항 목록 (사용자, 게시 공지만)
     * GET /api/notice
     */
    async getNotices(params = {}, isLoading) {
        return await ajax.getAjax(params, `${BASE_URL}`, isLoading);
    },

    /**
     * 공지사항 상세 (사용자, 조회수 증가)
     * GET /api/notice/${notice-id}
     */
    async getNoticeDetail(id, isLoading) {
        return await ajax.getAjax({}, `${BASE_URL}/${id}`, isLoading);
    },

    /**
     * 공지사항 목록 (관리자)
     * GET /api/admin/notice
     */
    async getAdminNotices(params = {}, isLoading) {
        return await ajax.getAjax(params, `${ADMIN_URL}`, isLoading);
    },

    /**
     * 공지사항 상세 (관리자)
     * GET /api/admin/notice/${notice-id}
     */
    async getAdminNoticeDetail(id, isLoading) {
        return await ajax.getAjax({}, `${ADMIN_URL}/${id}`, isLoading);
    },

    /**
     * 공지사항 등록 (관리자)
     * POST /api/admin/notice
     */
    async createNotice(data, isLoading) {
        return await ajax.postAjax(data, `${ADMIN_URL}`, isLoading);
    },

    /**
     * 공지사항 수정 (관리자)
     * PUT /api/admin/notice/${notice-id}
     */
    async updateNotice(id, data, isLoading) {
        return await ajax.putAjax(data, `${ADMIN_URL}/${id}`, isLoading);
    },

    /**
     * 공지사항 삭제 (관리자, 숨김 처리)
     * DELETE /api/admin/notice/${notice-id}
     */
    async deleteNotice(id, isLoading) {
        return await ajax.deleteAjax({}, `${ADMIN_URL}/${id}`, isLoading);
    },

};

export default noticeApi;
