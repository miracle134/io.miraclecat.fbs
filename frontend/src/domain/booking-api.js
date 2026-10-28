/**
 * packageName    : static.js.domain
 * fileName       : booking-api
 * date           : 2026-05-19
 * description    : 예약 API (사용자 /api/booking, 관리자 /api/admin/booking)
 */
// Utils
import {ajax} from '@core/core.js';

const BASE_URL = '/api/booking';
const ADMIN_URL = '/api/admin/booking';

const bookingApi = {

    /**
     * 예약 세션 생성
     * POST /api/booking/session
     */
    async createSession(data, isLoading) {
        return await ajax.postAjax(data, `${BASE_URL}/session`, isLoading);
    },

    /**
     * 예약 세션 조회
     * GET /api/booking/session/${session-id}
     */
    async getSession(id, isLoading) {
        return await ajax.getAjax({}, `${BASE_URL}/session/${id}`, isLoading);
    },

    /**
     * 예약 세션 삭제
     * DELETE /api/booking/session/${session-id}
     */
    async deleteSession(id, isLoading) {
        return await ajax.deleteAjax({}, `${BASE_URL}/session/${id}`, isLoading);
    },

    /**
     * 내 예약 목록 조회 (filter: all / upcoming / completed / cancelled, page: 0부터)
     * GET /api/booking/my
     */
    async getMyBookings(params = {}, isLoading) {
        return await ajax.getAjax(params, `${BASE_URL}/my`, isLoading);
    },

    /**
     * 좌석 변경
     * POST /api/booking/${booking-id}/seats
     */
    async changeSeats(id, data, isLoading) {
        return await ajax.postAjax(data, `${BASE_URL}/${id}/seats`, isLoading);
    },

    /**
     * 예약 취소 (사용자)
     * PUT /api/booking/${booking-id}/cancel
     */
    async cancelBooking(id, data, isLoading) {
        return await ajax.putAjax(data, `${BASE_URL}/${id}/cancel`, isLoading);
    },

    /**
     * 예약 목록 (관리자)
     * GET /api/admin/booking
     */
    async getAdminBookings(params = {}, isLoading) {
        return await ajax.getAjax(params, `${ADMIN_URL}`, isLoading);
    },

    /**
     * 예약 상세 (관리자)
     * GET /api/admin/booking/${booking-id}
     */
    async getAdminBookingDetail(id, isLoading) {
        return await ajax.getAjax({}, `${ADMIN_URL}/${id}`, isLoading);
    },

    /**
     * 예약 취소 (관리자, refundType: FULL / FARE)
     * PUT /api/admin/booking/${booking-id}/cancel
     */
    async cancelByAdmin(id, data, isLoading) {
        return await ajax.putAjax(data, `${ADMIN_URL}/${id}/cancel`, isLoading);
    },

};

export default bookingApi;
