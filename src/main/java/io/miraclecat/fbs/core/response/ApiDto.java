package io.miraclecat.fbs.core.response;

import lombok.*;
import lombok.extern.slf4j.Slf4j;

/**
 * packageName    : io.miraclecat.fbs.auth.utils
 * fileName       : ApiDto
 * author         : MiracleCat 
 * date           : 2024-05-02
 * description    : JSON Default Body
 */
@Slf4j
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiDto<T> {

    private int status;
    private String code;
    private String message;
    private T result;
    
    public static <T> ApiDto<T> of(HttpsStatusCode httpsStatusCode, T result) {
        return of(httpsStatusCode, httpsStatusCode.getMessage(), result);
    }

    public static <T> ApiDto<T> of(HttpsStatusCode httpsStatusCode, String message, T result) {
        return ApiDto.<T>builder()
            .status(httpsStatusCode.getViewStatus())
            .code(httpsStatusCode.name())
            .message(message)
            .result(result)
            .build();
    }
    
    public static <T> ApiDto<T> ok(T result) {
        return of(HttpsStatusCode.SUCCESS, result);
    }
    
    public static <T> ApiDto<T> empty(HttpsStatusCode httpsStatusCode) {
        return of(httpsStatusCode, null);
    }
    
}
