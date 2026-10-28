/**
 * packageName    : static.js.domain
 * fileName       : flight-api
 * date           : 2026-05-19
 * description    : 항공편 API
 */
// Utils
import {ajax} from '@core/core.js';

const BASE_URL = '/api/flight';
const ADMIN_URL = '/api/admin/flight';

const flightApi = {

    /**
     * 항공편 검색
     * GET /api/flight/search
     */
    async getFlightsSearch(params = {}, isLoading) {
        return await ajax.getAjax(params, `${BASE_URL}/search`, isLoading);
    },

    /**
     * 항공편 상세 조회
     * GET /api/flight/${flight-id}
     */
    async getFlightDetail(id, isLoading) {
        return await ajax.getAjax({}, `${BASE_URL}/${id}`, isLoading);
    },

    /**
     * 재고 상세 조회
     * GET /api/flight/inventory/${inventory-id}
     */
    async getInventoryDetail(id, isLoading) {
        return await ajax.getAjax({}, `${BASE_URL}/inventory/${id}`, isLoading);
    },

    /**
     * 좌석 상태 조회 (예약 확정 + 임시 선점)
     * GET /api/flight/inventory/${inventory-id}/seats
     */
    async getSeatStatus(id, isLoading) {
        return await ajax.getAjax({}, `${BASE_URL}/inventory/${id}/seats`, isLoading);
    },

    /**
     * 좌석 임시 선점
     * POST /api/flight/inventory/${inventory-id}/seats/${seat-number}/hold
     */
    async holdSeat(id, seatNumber, data, isLoading) {
        return await ajax.postAjax(data, `${BASE_URL}/inventory/${id}/seats/${seatNumber}/hold`, isLoading);
    },

    /**
     * 내 좌석 선점 일괄 연장
     * POST /api/flight/inventory/${inventory-id}/seats/hold/extend
     */
    async extendSeatHolds(id, isLoading) {
        return await ajax.postAjax({}, `${BASE_URL}/inventory/${id}/seats/hold/extend`, isLoading);
    },

    /**
     * 좌석 임시 선점 해제
     * DELETE /api/flight/inventory/${inventory-id}/seats/${seat-number}/hold
     */
    async releaseSeat(id, seatNumber, data, isLoading) {
        return await ajax.deleteAjax(data, `${BASE_URL}/inventory/${id}/seats/${seatNumber}/hold`, isLoading);
    },

    /**
     * 항공편 목록 (관리자)
     * GET /api/admin/flight
     */
    async getAdminFlights(params = {}, isLoading) {
        return await ajax.getAjax(params, `${ADMIN_URL}`, isLoading);
    },

    /**
     * 항공편 상세 (관리자, 재고 현황 포함)
     * GET /api/admin/flight/${flight-id}
     */
    async getAdminFlightDetail(id, isLoading) {
        return await ajax.getAjax({}, `${ADMIN_URL}/${id}`, isLoading);
    },

    /**
     * 항공편 등록 (관리자)
     * POST /api/admin/flight
     */
    async createFlight(data, isLoading) {
        return await ajax.postAjax(data, `${ADMIN_URL}`, isLoading);
    },

    /**
     * 항공편 수정 (관리자)
     * PUT /api/admin/flight/${flight-id}
     */
    async updateFlight(id, data, isLoading) {
        return await ajax.putAjax(data, `${ADMIN_URL}/${id}`, isLoading);
    },

    /**
     * 항공편 운항 중지 (관리자)
     * DELETE /api/admin/flight/${flight-id}
     */
    async deleteFlight(id, isLoading) {
        return await ajax.deleteAjax({}, `${ADMIN_URL}/${id}`, isLoading);
    },

    /**
     * 재고 일괄 생성 (관리자)
     * POST /api/admin/flight/${flight-id}/inventory/generate
     */
    async generateInventory(id, data, isLoading) {
        return await ajax.postAjax(data, `${ADMIN_URL}/${id}/inventory/generate`, isLoading);
    },

    /**
     * 재고 수동 조정 (관리자)
     * PUT /api/admin/flight/inventory/${inventory-id}
     */
    async adjustInventory(id, data, isLoading) {
        return await ajax.putAjax(data, `${ADMIN_URL}/inventory/${id}`, isLoading);
    },

};

export default flightApi;
