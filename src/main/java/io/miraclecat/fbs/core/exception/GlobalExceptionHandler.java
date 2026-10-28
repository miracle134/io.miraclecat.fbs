package io.miraclecat.fbs.core.exception;

import io.miraclecat.fbs.core.response.ApiEntity;
import io.miraclecat.fbs.core.response.HttpsStatusCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * packageName    : io.miraclecat.fbs.auth.handler
 * fileName       : DefaultExceptionHandler
 * author         : MiracleCat 
 * date           : 2024-05-02
 * description    :
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	/**
	 * HTTP or API Check Method
	 */
    private boolean isApiRequest(HttpServletRequest request) {
        String acceptHeader = request.getHeader("Accept");
        return acceptHeader != null &&
               (acceptHeader.contains("application/json") ||
                request.getRequestURI().startsWith("/api/"));
    }

	/**
	 * 인증 기본 예외 처리
	 */
	@ExceptionHandler(AuthException.class)
	protected Object handleAuthException(AuthException ex, HttpServletRequest request) {
		log.warn("AuthException occurred: {}", ex.getMessage());
        if (isApiRequest(request)) {
            return ApiEntity.of(ex.getStatusCode());
        } else {
            return new ModelAndView("redirect:/denied");
        }
	}
	
	/**
	 * 애플리케이션 기본 예외 처리
	 */
	@ExceptionHandler(BaseException.class)
	protected Object handleBaseException(BaseException ex, HttpServletRequest request) {
		log.warn("BaseException occurred: {}", ex.getMessage(), ex);
        if (isApiRequest(request)) {
            // 4xx: 서비스에서 지정한 안내 메시지 / 5xx: 내부 오류 내용 노출 방지를 위해 상태 코드 기본 메시지
            HttpsStatusCode statusCode = ex.getStatusCode();
            String message = statusCode.isClientError() && ex.getMessage() != null
                    ? ex.getMessage()
                    : statusCode.getMessage();
            return ApiEntity.ofMessage(statusCode, message);
        } else {
            return new ModelAndView("redirect:/denied");
        }
	}
	
	/**
	 * 데이터 검증 예외 처리
	 */
	@ExceptionHandler(ValidationException.class)
	protected Object handleValidationException(ValidationException ex, HttpServletRequest request) {
		log.warn("ValidationException occurred: {}", ex.getMessage());
        if (isApiRequest(request)) {
            return ApiEntity.of(ex.getStatusCode(), ex.getFieldErrors());
        } else {
            return new ModelAndView("redirect:/denied");
        }
	}
	
	/**
	 * Spring Validation 예외 처리
	 */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	protected Object handleMethodArgumentNotValid(
		MethodArgumentNotValidException ex, HttpServletRequest request) {
		log.warn("MethodArgumentNotValidException occurred");
		
		Map<String, String> fieldErrors = ex.getBindingResult()
			.getFieldErrors()
			.stream()
			.collect(Collectors.toMap(
				FieldError::getField,
				fieldError -> Optional.ofNullable(fieldError.getDefaultMessage()).orElse(""),
				(existing, replacement) -> existing
			));
		
        if (isApiRequest(request)) {
            return ApiEntity.of(HttpsStatusCode.INVALID_PARAMETER, fieldErrors);
        } else {
            return new ModelAndView("redirect:/denied");
        }
	}
	
	/**
	 * 바인딩 에러 처리
	 */
	@ExceptionHandler(BindException.class)
	protected Object handleBindException(BindException ex, HttpServletRequest request) {
		log.warn("BindException occurred");
		
		Map<String, String> fieldErrors = ex.getBindingResult()
			.getFieldErrors()
			.stream()
			.collect(Collectors.toMap(
				FieldError::getField,
				fieldError -> Optional.ofNullable(fieldError.getDefaultMessage()).orElse(""),
				(existing, replacement) -> existing
			));
		
        if (isApiRequest(request)) {
            return ApiEntity.of(HttpsStatusCode.INVALID_PARAMETER, fieldErrors);
        } else {
            return new ModelAndView("redirect:/denied");
        }
	}
	
	/**
	 * 타입 미스매치 예외
	 */
	@ExceptionHandler(TypeMismatchException.class)
	protected Object handleTypeMismatchException(TypeMismatchException ex, HttpServletRequest request) {
		log.warn("TypeMismatchException occurred: {}", ex.getMessage());
        if (isApiRequest(request)) {
            return ApiEntity.of(HttpsStatusCode.INVALID_PARAMETER);
        } else {
            return new ModelAndView("redirect:/denied");
        }
	}
	
	/**
	 * 리소스 없음 예외
	 */
	@ExceptionHandler(NoHandlerFoundException.class)
	protected Object handleNoHandlerFound(NoHandlerFoundException ex, HttpServletRequest request,
                                          RedirectAttributes redirectAttributes) {
		log.warn("NoHandlerFoundException: {}", ex.getRequestURL());
        if (isApiRequest(request)) {
            return ApiEntity.of(HttpsStatusCode.RESOURCE_NOT_FOUND);
        } else {
            redirectAttributes.addFlashAttribute("status", 404);
            redirectAttributes.addFlashAttribute("uri", request.getRequestURI());
            return new ModelAndView("redirect:/error");
        }
	}

	/**
	 * 정적 리소스 없음 예외 (소스맵 등)
	 */
	@ExceptionHandler(NoResourceFoundException.class)
	protected ApiEntity<Void> handleNoResourceFound(NoResourceFoundException ex) {
		String resourcePath = ex.getResourcePath();
		// .map, favicon.ico 등은 로그 미출력
		if (resourcePath != null && (resourcePath.endsWith(".map") || resourcePath.contains("favicon"))) {
			return ApiEntity.of(HttpsStatusCode.RESOURCE_NOT_FOUND);
		}
		log.warn("NoResourceFoundException: {}", resourcePath);
		return ApiEntity.of(HttpsStatusCode.RESOURCE_NOT_FOUND);
	}

	/**
	 * HTTP 메서드 지원 안함
	 */
	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	protected Object handleHttpRequestMethodNotSupported(
		HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
		log.warn("HttpRequestMethodNotSupportedException: {}", ex.getMessage());
        if (isApiRequest(request)) {
            return ApiEntity.of(HttpsStatusCode.INVALID_PARAMETER);
        } else {
            return new ModelAndView("redirect:/denied");
        }
	}
	
	/**
	 * 요청 본문 읽기 실패
	 */
	@ExceptionHandler(HttpMessageNotReadableException.class)
	protected Object handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
		log.warn("HttpMessageNotReadableException occurred: {}", ex.getMessage());
        if (isApiRequest(request)) {
            return ApiEntity.of(HttpsStatusCode.INVALID_PARAMETER);
        } else {
            return new ModelAndView("redirect:/denied");
        }
	}
	
	/**
	 * 필수 파라미터 누락
	 */
	@ExceptionHandler(MissingServletRequestParameterException.class)
	protected Object handleMissingServletRequestParameter(
		MissingServletRequestParameterException ex, HttpServletRequest request) {
		log.warn("MissingServletRequestParameterException: {}", ex.getMessage());
        if (isApiRequest(request)) {
            return ApiEntity.of(HttpsStatusCode.INVALID_PARAMETER);
        } else {
            return new ModelAndView("redirect:/denied");
        }
	}
	
	/**
	 * 접근 거부
	 */
	@ExceptionHandler(AccessDeniedException.class)
	protected Object handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
		log.warn("AccessDeniedException occurred: {}", ex.getMessage());
        if (isApiRequest(request)) {
            return ApiEntity.of(HttpsStatusCode.ACCESS_DENIED);
        } else {
            return new ModelAndView("redirect:/denied");
        }
	}

	/**
	 * 데이터베이스 제약조건 위반
	 */
	@ExceptionHandler(DataIntegrityViolationException.class)
	protected Object handleDataIntegrityViolation(DataIntegrityViolationException ex, HttpServletRequest request) {
		log.warn("DataIntegrityViolationException occurred", ex);
        if (isApiRequest(request)) {
            return ApiEntity.of(HttpsStatusCode.INVALID_PARAMETER);
        } else {
            return new ModelAndView("redirect:/denied");
        }
	}
	
	/**
	 * 알 수 없는 예외 처리 (최후의 방어선)
	 */
	@ExceptionHandler(Exception.class)
	protected Object handleException(Exception ex, HttpServletRequest request) {
		log.error("Unexpected exception occurred", ex);
        if (isApiRequest(request)) {
            return ApiEntity.of(HttpsStatusCode.INTERNAL_SERVER_ERROR);
        } else {
            return new ModelAndView("redirect:/denied");
        }
	}

}
