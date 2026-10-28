package io.miraclecat.fbs.core.exception;

import io.miraclecat.fbs.core.response.HttpsStatusCode;

/**
 * packageName    : io.miraclecat.fbs.core.exception
 * fileName       : BusinessException
 * author         : MiracleCat
 * date           : 25. 8. 26.
 * description    :
 */
public class BusinessException extends BaseException {
	public BusinessException(String message, HttpsStatusCode statusCode) {
		super(message, statusCode);
	}
	
	public BusinessException(HttpsStatusCode statusCode) {
		super(statusCode.getMessage(), statusCode);
	}
}