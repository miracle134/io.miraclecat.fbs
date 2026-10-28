package io.miraclecat.fbs.core.exception;

import io.miraclecat.fbs.core.response.HttpsStatusCode;
import lombok.Getter;

/**
 * packageName    : io.miraclecat.fbs.core.exception
 * fileName       : DefaultSingleException
 * author         : MiracleCat 
 * date           : 2024-10-08
 * description    : 기본 예외처리
 */
public abstract class BaseException extends RuntimeException {
	@Getter
	private final HttpsStatusCode statusCode;
	
	protected BaseException(String message, HttpsStatusCode statusCode) {
		super(message);
		this.statusCode = statusCode;
	}
	
	protected BaseException(String message, Throwable cause, HttpsStatusCode statusCode) {
		super(message, cause);
		this.statusCode = statusCode;
	}
}