package io.miraclecat.fbs.core.response;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * packageName    : io.miraclecat.fbs.auth.utils
 * fileName       : ApiEntity
 * author         : MiracleCat 
 * date           : 2024-05-02
 * description    : Custom ResponseEntity
 */
public class ApiEntity<T> extends ResponseEntity<ApiDto<T>> {

    public ApiEntity(HttpsStatusCode httpsStatusCode, T result) {
        super(ApiDto.of(httpsStatusCode, result), httpsStatusCode.getStatus());
    }

    public ApiEntity(T result) {
        super(ApiDto.ok(result), HttpStatus.OK);
    }
    
    public ApiEntity(HttpsStatusCode httpsStatusCode) {
        super(ApiDto.empty(httpsStatusCode), httpsStatusCode.getStatus());
    }

    public ApiEntity(HttpsStatusCode httpsStatusCode, String message, T result) {
        super(ApiDto.of(httpsStatusCode, message, result), httpsStatusCode.getStatus());
    }

    /**
     * 메시지 지정 응답
     * @param httpsStatusCode status
     * @param message message
     */
    public static <T> ApiEntity<T> ofMessage(HttpsStatusCode httpsStatusCode, String message) {
        return new ApiEntity<>(httpsStatusCode, message, null);
    }
    
    /**
     * Default API Response
     * @param httpsStatusCode status
     * @param result data
     */
    public static <T> ApiEntity<T> of(HttpsStatusCode httpsStatusCode, T result) {
        return new ApiEntity<>(httpsStatusCode, result);
    }
    
    /**
     * Default Code 200
     * @param result data
     */
    public static <T> ApiEntity<T> of(T result) {
        return new ApiEntity<>(result);
    }
    
    /**
     * Default Empty
     * @param httpsStatusCode status
     */
    public static <T> ApiEntity<T> of(HttpsStatusCode httpsStatusCode) {
        return new ApiEntity<>(httpsStatusCode);
    }

}