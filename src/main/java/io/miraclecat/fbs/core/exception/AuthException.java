package io.miraclecat.fbs.core.exception;

import io.miraclecat.fbs.core.response.HttpsStatusCode;

/**
 * packageName    : io.miraclecat.fbs.core.exception
 * fileName       : AuthException
 * author         : MiracleCat 
 * date           : 25. 8. 26.
 * description    :
 */
public class AuthException extends BaseException {
    public AuthException(String message, HttpsStatusCode statusCode) {
        super(message, statusCode);
    }
    
    public AuthException(HttpsStatusCode statusCode) {
        super(statusCode.getMessage(), statusCode);
    }
}