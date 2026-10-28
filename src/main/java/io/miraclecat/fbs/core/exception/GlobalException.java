package io.miraclecat.fbs.core.exception;

import io.miraclecat.fbs.core.response.HttpsStatusCode;

import java.util.Map;

/**
 * packageName    : io.miraclecat.fbs.core.utils.support
 * fileName       : ExceptionUtil
 * author         : MiracleCat
 * date           : 25. 8. 26.
 * description    :
 */
public final class GlobalException {

    /**
     * private 생성자로 인스턴스화 방지
     */
    private GlobalException() {
        throw new IllegalStateException("Utility class");
    }
	
    // 장점: 간단하고 빠르게 예외 생성
    public static BusinessException business(String message, HttpsStatusCode statusCode) {
        return new BusinessException(message, statusCode);
    }
    
    public static BusinessException business(HttpsStatusCode statusCode) {
        return new BusinessException(statusCode);
    }
    
    public static AuthException auth(HttpsStatusCode statusCode) {
        return new AuthException(statusCode);
    }
    
    public static ValidationException validation(String message, Map<String, String> fieldErrors) {
        return new ValidationException(message, fieldErrors);
    }
}