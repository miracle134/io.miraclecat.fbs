/**
 * packageName    : io.miraclecat.fbs.api.payment.controller
 * fileName       : PaymentRestController
 * author         : MiracleCat
 * date           : 2026-05-19
 * description    : 결제 컨트롤러
 */
package io.miraclecat.fbs.api.payment.controller;

import io.miraclecat.fbs.api.payment.dto.PaymentDto;
import io.miraclecat.fbs.api.payment.service.PaymentCheckoutService;
import io.miraclecat.fbs.core.response.ApiEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PaymentRestController {

    private final PaymentCheckoutService paymentCheckoutService;

    /**
     * 결제 준비 (결제하기 클릭 → 토스 결제창 호출 값 발급)
     * @param req req
     * @return ApiEntity
     */
    @PostMapping("/api/payment/prepare")
    public ApiEntity<?> preparePayment(@RequestBody PaymentDto.PrepareReq req) {
        return paymentCheckoutService.prepare(req);
    }

    /**
     * 결제 승인 (토스 successUrl 복귀 후)
     * @param req req
     * @return ApiEntity
     */
    @PostMapping("/api/payment/confirm")
    public ApiEntity<?> confirmPayment(@RequestBody PaymentDto.ConfirmReq req) {
        return paymentCheckoutService.confirm(req);
    }

}
