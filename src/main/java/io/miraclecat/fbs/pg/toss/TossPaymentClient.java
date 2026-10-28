/**
 * packageName    : io.miraclecat.fbs.pg.toss
 * fileName       : TossPaymentClient
 * author         : MiracleCat
 * date           : 2026-05-19
 * description    : 토스페이먼츠 API HTTP 클라이언트
 *                  - 오류 응답(4xx/5xx)은 TossApiException(code, message)으로 변환
 *                  - 응답 대기 30초 초과 시 IllegalStateException (승인 여부 불명확 → 호출부에서 기술 실패로 처리)
 */
package io.miraclecat.fbs.pg.toss;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

@Slf4j
@Component
@RequiredArgsConstructor
public class TossPaymentClient {

    private static final String TOSS_API_URL = "https://api.tosspayments.com/v1/payments";
    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    @Value("${toss.secret-key}")
    private String secretKey;

    private WebClient webClient() {
        // 토스 인증: Base64(secretKey:)
        String encoded = Base64.getEncoder()
                .encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));

        return WebClient.builder()
                .baseUrl(TOSS_API_URL)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + encoded)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * 결제 승인 요청
     */
    public TossPaymentDto.ConfirmRes confirm(TossPaymentDto.ConfirmReq req) {
        return webClient()
                .post()
                .uri("/confirm")
                .bodyValue(req)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::toException)
                .bodyToMono(TossPaymentDto.ConfirmRes.class)
                .block(TIMEOUT);
    }

    /**
     * 결제 취소 요청
     * @param idempotencyKey 같은 키로 재요청 시 중복 취소되지 않음
     */
    public TossPaymentDto.CancelRes cancel(String paymentKey, TossPaymentDto.CancelReq req, String idempotencyKey) {
        return webClient()
                .post()
                .uri("/{paymentKey}/cancel", paymentKey)
                .header("Idempotency-Key", idempotencyKey)
                .bodyValue(req)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::toException)
                .bodyToMono(TossPaymentDto.CancelRes.class)
                .block(TIMEOUT);
    }

    private Mono<Throwable> toException(ClientResponse response) {
        int status = response.statusCode().value();
        return response.bodyToMono(TossPaymentDto.ErrorRes.class)
                .defaultIfEmpty(new TossPaymentDto.ErrorRes())
                .map(error -> {
                    log.warn("토스 API 오류 - status: {}, code: {}, message: {}", status, error.getCode(), error.getMessage());
                    return new TossApiException(status, error.getCode(), error.getMessage());
                });
    }

}
