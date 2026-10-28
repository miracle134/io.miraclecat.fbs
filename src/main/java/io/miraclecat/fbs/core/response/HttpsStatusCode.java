package io.miraclecat.fbs.core.response;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.Arrays;
import java.util.Optional;

/**
 * packageName    : io.miraclecat.fbs.auth.utils
 * fileName       : HttpsStatusCode
 * author         : MiracleCat
 * date           : 2024-05-02
 * description    :
 */
@Getter
public enum HttpsStatusCode {
	
	/**
	 * 2xx Success
	 */
	SUCCESS(HttpStatus.OK, 200, "처리되었습니다."), // 기본 성공
	CLEAR_CACHE_SUCCESS(HttpStatus.OK, 200, "캐시가 초기화되었습니다."), // 캐시 초기화 완료
	REGISTER(HttpStatus.CREATED, 201, "등록되었습니다."), // 기본 등록 성공
	ACCEPTED(HttpStatus.ACCEPTED, 202, "요청이 접수되었습니다."), // 요청 수락됨
	DATA_EMPTY(HttpStatus.NO_CONTENT, 204, "데이터가 없습니다."), // 기본 빈 데이터
	
	/**
	 * 4xx Client Error
	 */
	INVALID_PARAMETER(HttpStatus.BAD_REQUEST, 400, "요청 값이 올바르지 않습니다."), // 기본 유효하지 않은 매개변수
	INVALID_ACCOUNT_STATUS(HttpStatus.BAD_REQUEST, 400, "사용할 수 없는 계정 상태입니다."), // 기본 계정 인증 에러
	INVALID_API_KEY(HttpStatus.UNAUTHORIZED, 401, "API 키가 없거나 올바르지 않습니다."), // API Key 불일치
	ID_PW_NOT_FOUND(HttpStatus.UNAUTHORIZED, 401, "아이디와 비밀번호를 확인해주세요."), // 계정 정보 불일치
	ACCESS_DENIED(HttpStatus.FORBIDDEN, 403, "접근 권한이 없습니다."), // 권한 없는 요청
	JWT_EXPIRED(HttpStatus.FORBIDDEN, 403, "로그인이 만료되었습니다. 다시 로그인해주세요."), // JWT 토큰 만료
	LOGIN_REQUIRED(HttpStatus.FORBIDDEN, 403, "로그인이 필요합니다."), // 비로그인 요청
	RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, 404, "요청한 정보를 찾을 수 없습니다."), // 기본 Resource 찾을 수 없음
	MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, 404, "계정을 찾을 수 없습니다."), // 계정을 찾을 수 없음
	METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, 405, "허용되지 않은 요청 방식입니다."), // 허용되지 않은 HTTP 메서드
	NOT_ACCEPTABLE(HttpStatus.NOT_ACCEPTABLE, 406, "지원하지 않는 응답 형식입니다."), // 요청 Accept 헤더와 맞지 않음
	DUPLICATE_ITEM(HttpStatus.CONFLICT, 409, "이미 존재하는 데이터입니다."), // 중복된 ID
	UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, 415, "지원하지 않는 요청 형식입니다."), // 지원하지 않는 미디어 타입
	UNPROCESSABLE_ENTITY(HttpStatus.UNPROCESSABLE_ENTITY, 422, "요청을 처리할 수 없습니다."), // 처리할 수 없는 엔티티
	TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, 429, "요청이 너무 많습니다. 잠시 후 다시 시도해주세요."), // 너무 많은 요청
	
	/**
	 * 5xx Server Error
	 */
	INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, 500, "일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요."), // 기본 서버 에러
	NOT_IMPLEMENTED(HttpStatus.NOT_IMPLEMENTED, 501, "지원하지 않는 기능입니다."), // 구현되지 않음
	BAD_GATEWAY(HttpStatus.BAD_GATEWAY, 502, "외부 서비스 연결에 실패했습니다."), // 잘못된 게이트웨이
	SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, 503, "서비스를 일시적으로 사용할 수 없습니다."), // 서비스 사용 불가
	GATEWAY_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, 504, "외부 서비스 응답이 지연되고 있습니다."), // 게이트웨이 타임아웃
	PAYMENT_BOOKING_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 500, "결제는 완료되었으나 예약 처리 중 오류가 발생했습니다. 고객센터로 문의해주세요."), // 결제 승인 후 예약 저장 실패
	PAYMENT_CONFIRM_UNKNOWN(HttpStatus.GATEWAY_TIMEOUT, 504, "결제 승인 결과를 확인하지 못했습니다. 고객센터로 문의해주세요."), // 결제 승인 응답 불명
	
	/**
	 * Custom Status Codes
	 */
	REQUEST_TIMEOUT(HttpStatus.REQUEST_TIMEOUT, 408, "요청 시간이 초과되었습니다."), // 요청 타임아웃
	MEMBER_DATA_EMPTY(HttpStatus.NO_CONTENT, 204, "계정 정보가 없습니다."), // 계정 데이터 비어있음
	
	/**
	 * HTTP Client Error
	 */
	HTTP_CLIENT_ERROR(HttpStatus.BAD_REQUEST, 400, "외부 요청 처리 중 오류가 발생했습니다."), // HTTP 클라이언트 에러
	CONNECTION_TIMEOUT(HttpStatus.REQUEST_TIMEOUT, 408, "연결 시간이 초과되었습니다."), // 연결 타임아웃
	SSL_HANDSHAKE_FAILED(HttpStatus.BAD_REQUEST, 400, "보안 연결에 실패했습니다."), // SSL 핸드셰이크 실패

    /**
     * API 키 관련 오류
     */
    API_KEY_EXPIRED(HttpStatus.UNAUTHORIZED, 401, "API 키가 만료되었습니다."), // API Key 만료
    API_QUOTA_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, 429, "API 사용량을 초과했습니다."), // API 할당량 초과
    API_RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, 429, "API 요청 횟수 제한을 초과했습니다."), // API 요청 제한 초과

	;
	
	private final HttpStatus status;
	private final int viewStatus;
	private final String message;
	
	/**
	 * @param status HttpStatus Code
	 * @param viewStatus 사용자 보여줄 Status Code
	 * @param message 메세지
	 */
	HttpsStatusCode(HttpStatus status, int viewStatus, String message) {
		this.status = status;
		this.viewStatus = viewStatus;
		this.message = message;
	}
	
	/**
	 * HTTP 상태 코드로 HttpsStatusCode를 찾습니다.
	 *
	 * @param statusCode HTTP 상태 코드
	 * @return 해당하는 HttpsStatusCode, 없으면 적절한 기본값 반환
	 */
	public static HttpsStatusCode fromCode(int statusCode) {
		// 먼저 정확히 일치하는 코드를 찾습니다
		Optional<HttpsStatusCode> exactMatch = Arrays.stream(values())
			.filter(code -> code.getViewStatus() == statusCode)
			.findFirst();
		
		if (exactMatch.isPresent()) {
			return exactMatch.get();
		}
		
		// 정확한 매치가 없으면 상태 코드 범위에 따라 기본값을 반환합니다
		if (statusCode >= 200 && statusCode < 300) {
			return SUCCESS;
		} else if (statusCode >= 300 && statusCode < 400) {
			// 3xx Redirection - 기본적으로 성공으로 처리하거나 별도 처리 필요시 추가
			return SUCCESS;
		} else if (statusCode >= 400 && statusCode < 500) {
			// 4xx Client Error
			switch (statusCode) {
				case 400: return INVALID_PARAMETER;
				case 401: return INVALID_API_KEY;
				case 403: return ACCESS_DENIED;
				case 404: return RESOURCE_NOT_FOUND;
				case 405: return METHOD_NOT_ALLOWED;
				case 406: return NOT_ACCEPTABLE;
				case 408: return REQUEST_TIMEOUT;
				case 409: return DUPLICATE_ITEM;
				case 415: return UNSUPPORTED_MEDIA_TYPE;
				case 422: return UNPROCESSABLE_ENTITY;
				case 429: return TOO_MANY_REQUESTS;
				default: return INVALID_PARAMETER;
			}
		} else if (statusCode >= 500 && statusCode < 600) {
			// 5xx Server Error
			switch (statusCode) {
				case 500: return INTERNAL_SERVER_ERROR;
				case 501: return NOT_IMPLEMENTED;
				case 502: return BAD_GATEWAY;
				case 503: return SERVICE_UNAVAILABLE;
				case 504: return GATEWAY_TIMEOUT;
				default: return INTERNAL_SERVER_ERROR;
			}
		}
		
		// 알 수 없는 상태 코드의 경우 기본값 반환
		return INTERNAL_SERVER_ERROR;
	}
	
	/**
	 * HttpStatus로 HttpsStatusCode를 찾습니다.
	 *
	 * @param httpStatus Spring의 HttpStatus
	 * @return 해당하는 HttpsStatusCode
	 */
	public static HttpsStatusCode fromHttpStatus(HttpStatus httpStatus) {
		return fromCode(httpStatus.value());
	}
	
	/**
	 * 상태 코드가 성공 범위(2xx)인지 확인합니다.
	 *
	 * @return 성공 상태 코드이면 true
	 */
	public boolean isSuccess() {
		return viewStatus >= 200 && viewStatus < 300;
	}
	
	/**
	 * 상태 코드가 클라이언트 에러 범위(4xx)인지 확인합니다.
	 *
	 * @return 클라이언트 에러 상태 코드이면 true
	 */
	public boolean isClientError() {
		return viewStatus >= 400 && viewStatus < 500;
	}
	
	/**
	 * 상태 코드가 서버 에러 범위(5xx)인지 확인합니다.
	 *
	 * @return 서버 에러 상태 코드이면 true
	 */
	public boolean isServerError() {
		return viewStatus >= 500 && viewStatus < 600;
	}
	
	/**
	 * 상태 코드가 에러 범위(4xx, 5xx)인지 확인합니다.
	 *
	 * @return 에러 상태 코드이면 true
	 */
	public boolean isError() {
		return isClientError() || isServerError();
	}
}