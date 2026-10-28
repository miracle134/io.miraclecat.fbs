/**
 * packageName    : static.js.domain
 * fileName       : payment-api
 * date           : 2026-05-19
 * description    : 결제 API
 */
// Utils
import {ajax} from '@core/core.js';

const BASE_URL = '/api/payment';

const paymentApi = {

    /**
     * 결제 준비 (결제하기 → 토스 결제창 호출 값)
     * POST /api/payment/prepare
     */
    async preparePayment(data, isLoading) {
        return await ajax.postAjax(data, `${BASE_URL}/prepare`, isLoading);
    },

    /**
     * 결제 승인 (토스 successUrl 복귀 후 서버 승인)
     * POST /api/payment/confirm
     */
    async confirmPayment(data, isLoading) {
        return await ajax.postAjax(data, `${BASE_URL}/confirm`, isLoading);
    },

};

export default paymentApi;
