package io.miraclecat.fbs.core.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.miraclecat.fbs.core.auth.AuthRole;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * packageName    : io.miraclecat.fbs.auth.dto
 * fileName       : JwtDto
 * author         : MiracleCat 
 * date           : 2023-07-17
 * description    :
 */
@Getter
@Setter
@Builder
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JwtDto {

    private Long memberId; // 아이디
    private String loginId; // 로그인 아이디
    private String name; // 이름
    private AuthRole role; // 권한
    private AuthRole creatorRole; // 그룹 권한
    private String email; // 이메일
    private String rank; // 직급
    private String empId; // 사원번호
    private String dept; // 부서
    private String tel; // 연락처
    private String scope; // 토큰 분리

}
