package io.miraclecat.fbs.core.audit.authentication.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.miraclecat.fbs.core.generator.annotation.FieldMeta;
import io.miraclecat.fbs.core.generator.annotation.GenerateMeta;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Arrays;

/**
 * packageName    : io.miraclecat.fbs.core.audit.authentication.entity
 * fileName       : AuthenticationLog
 * author         : MiracleCat 
 * date           :
 * description    :
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(schema = "flight-booking", name = "authentication_log")
@Comment("로그인 기록")
@GenerateMeta(domain = "Audit")
public class AuthenticationLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @FieldMeta(insert = false)
    @Comment("로그인 기록 아이디")
    @Column(name = "authentication_log_id", length = 20, nullable = false)
    private Long authenticationLogId;

    @FieldMeta
    @Comment("회원 아이디")
    @Column(name = "member_id", length = 20)
    private Long memberId;

    @FieldMeta
    @Comment("로그인 아이디")
    @Column(name = "login_id", length = 64, nullable = false)
    private String loginId;

    @FieldMeta
    @Comment("이름")
    @Column(name = "member_name", length = 64)
    private String memberName;

    @FieldMeta
    @Comment("로그인 유형 (LOGIN, LOGOUT, LOGIN_FAIL, PASSWORD_CHANGE, ACCOUNT_LOCK)")
    @Column(name = "login_type", columnDefinition = "VARCHAR(32)")
    @Enumerated(EnumType.STRING)
    private LoginType loginType;

    @FieldMeta
    @Comment("IP 주소 (IPv6 지원)")
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @FieldMeta
    @Comment("브라우저 정보")
    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @FieldMeta
    @Comment("성공 여부")
    @Column(name = "is_success")
    @JdbcTypeCode(SqlTypes.TINYINT)
    @ColumnDefault("1")
    private Boolean isSuccess;

    @FieldMeta
    @Comment("실패 시 사유")
    @Column(name = "failure_reason", length = 200)
    private String failureReason;

    @Comment("등록일")
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * 아이디 설정
     */
    public static AuthenticationLog persistAuthenticationLog(Long authenticationLogId) {
        AuthenticationLog authenticationLog = new AuthenticationLog();
        authenticationLog.authenticationLogId = authenticationLogId;

        return authenticationLog;
    }

    @Builder(builderMethodName = "createAuthenticationLog")
    public AuthenticationLog(Long memberId, String loginId, String memberName, LoginType loginType, String ipAddress, String userAgent, Boolean isSuccess, String failureReason) {
        this.memberId = memberId;
        this.loginId = loginId;
        this.memberName = memberName;
        this.loginType = loginType;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.isSuccess = isSuccess;
        this.failureReason = failureReason;
        this.createdAt = LocalDateTime.now();
    }

    @Getter
    public enum LoginType {

        LOGIN("LOGIN", "로그인"),
        LOGOUT("LOGOUT", "로그아웃"),
        LOGIN_FAIL("LOGIN_FAIL", "로그인 실패"),
        PASSWORD_CHANGE("PASSWORD_CHANGE", "비밀번호 변경"),
        ACCOUNT_LOCK("ACCOUNT_LOCK", "계정 잠금");

        private final String label;
        private final String labelName;

        LoginType(String label, String labelName) {
            this.label = label;
            this.labelName = labelName;
        }

        @JsonCreator
        public static LoginType from(String label) {
            return Arrays.stream(LoginType.values())
                .filter(type -> type.getLabel().equals(label))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown login type: " + label));
        }

        @JsonValue
        public String getLabel() {
            return label;
        }
    }

}
