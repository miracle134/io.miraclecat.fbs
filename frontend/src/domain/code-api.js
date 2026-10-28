/**
 * packageName    : static.js.domain
 * fileName       : code-api
 * date           : 2026-05-19
 * description    : 코드 API (항공사, 공항)
 */
// Utils
import {ajax} from '@core/core.js';

const BASE_URL = '/api/code';

const codeApi = {

    /**
     * 항공사 목록 조회
     * GET /api/code/airlines
     */
    async getAirlines(params = {}, isLoading) {
        return await ajax.getAjax(params, `${BASE_URL}/airlines`, isLoading);
    },

    /**
     * 공항 목록 조회
     * GET /api/code/airports
     */
    async getAirports(params = {}, isLoading) {
        return await ajax.getAjax(params, `${BASE_URL}/airports`, isLoading);
    },

};

export default codeApi;
