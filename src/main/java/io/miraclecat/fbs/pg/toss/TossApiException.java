/**
 * packageName    : io.miraclecat.fbs.pg.toss
 * fileName       : TossApiException
 * author         : MiracleCat
 * date           : 2026-10-25
 * description    : 토스페이먼츠 API 오류 응답 (HTTP 4xx/5xx)
 */
package io.miraclecat.fbs.pg.toss;

import lombok.Getter;

@Getter
public class TossApiException extends RuntimeException {

    private final int httpStatus;
    private final String code;

    public TossApiException(int httpStatus, String code, String message) {
        super(message != null ? message : "토스페이먼츠 요청에 실패했습니다.");
        this.httpStatus = httpStatus;
        this.code = code;
    }

}
