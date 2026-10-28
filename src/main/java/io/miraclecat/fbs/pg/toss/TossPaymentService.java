/**
 * packageName    : io.miraclecat.fbs.pg.toss
 * fileName       : TossPaymentService
 * author         : MiracleCat
 * date           : 2026-05-19
 * description    : 토스페이먼츠 결제 서비스
 */
package io.miraclecat.fbs.pg.toss;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TossPaymentService {

    private final TossPaymentClient tossPaymentClient;

    /**
     * 결제 승인
     */
    public TossPaymentDto.ConfirmRes confirm(String paymentKey, String orderId, Integer amount) {
        log.info("토스 결제 승인 요청 - paymentKey: {}, orderId: {}, amount: {}", paymentKey, orderId, amount);

        TossPaymentDto.ConfirmReq req = TossPaymentDto.ConfirmReq.builder()
                .paymentKey(paymentKey)
                .orderId(orderId)
                .amount(amount)
                .build();

        TossPaymentDto.ConfirmRes res = tossPaymentClient.confirm(req);
        log.info("토스 결제 승인 완료 - paymentKey: {}, status: {}", res.getPaymentKey(), res.getStatus());

        return res;
    }

    /**
     * 결제 취소
     * @param cancelAmount   부분 취소 금액 (null이면 전액 취소)
     * @param idempotencyKey 중복 취소 방지 키
     */
    public TossPaymentDto.CancelRes cancel(String paymentKey, String cancelReason, Integer cancelAmount, String idempotencyKey) {
        log.info("토스 결제 취소 요청 - paymentKey: {}, amount: {}, reason: {}", paymentKey, cancelAmount, cancelReason);

        TossPaymentDto.CancelReq req = TossPaymentDto.CancelReq.builder()
                .cancelReason(cancelReason)
                .cancelAmount(cancelAmount)
                .build();

        TossPaymentDto.CancelRes res = tossPaymentClient.cancel(paymentKey, req, idempotencyKey);
        log.info("토스 결제 취소 완료 - paymentKey: {}, status: {}", res.getPaymentKey(), res.getStatus());

        return res;
    }

}
