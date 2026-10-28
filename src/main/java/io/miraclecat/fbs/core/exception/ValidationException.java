package io.miraclecat.fbs.core.exception;

import io.miraclecat.fbs.core.response.HttpsStatusCode;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * packageName    : io.miraclecat.fbs.core.exception
 * fileName       : ValidationException
 * author         : MiracleCat 
 * date           : 25. 8. 26.
 * description    :
 */
public class ValidationException extends BaseException {
	@Getter
	private final Map<String, String> fieldErrors;
	
	public ValidationException(String message, Map<String, String> fieldErrors) {
		super(message, HttpsStatusCode.INVALID_PARAMETER);
		this.fieldErrors = fieldErrors != null ? fieldErrors : new HashMap<>();
	}
}