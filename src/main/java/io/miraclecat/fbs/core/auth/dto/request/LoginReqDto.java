package io.miraclecat.fbs.core.auth.dto.request;

import lombok.Getter;
import lombok.Setter;

/**
 * packageName    : io.miraclecat.fbs.auth.dto.request
 * fileName       : RequestLoginDto
 * author         : MiracleCat
 * date           : 2023-07-07
 * description    :
 */
@Getter
@Setter
public class LoginReqDto {
    private String loginId;
    private String loginPassword;
}
