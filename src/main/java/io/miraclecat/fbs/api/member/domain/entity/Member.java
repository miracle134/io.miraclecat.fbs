package io.miraclecat.fbs.api.member.domain.entity;

import io.miraclecat.fbs.api.common.domain.BaseEntity;
import io.miraclecat.fbs.core.auth.AuthRole;
import io.miraclecat.fbs.core.generator.annotation.FieldMeta;
import io.miraclecat.fbs.core.generator.annotation.GenerateMeta;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;

/**
 * packageName    : io.miraclecat.fbs.domain
 * fileName       : Member
 * author         : MiracleCat
 * date           : 2025-04-09
 * description    :
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(schema = "flight-booking", name = "member",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_member_login_id", columnNames = "login_id"),
                @UniqueConstraint(name = "uk_member_provider", columnNames = {"provider", "provider_id"})
        })
@Comment("회원")
@GenerateMeta(domain = "Member")
public class Member extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @FieldMeta(insert = false)
    @Comment("회원 아이디")
    @Column(name = "member_id", length = 20, nullable = false)
    private Long memberId;

    @Comment("로그인 아이디")
    @Column(name = "login_id", nullable = false, length = 64)
    @FieldMeta(where = true)
    private String loginId;

    @Comment("비밀번호 (소셜은 NULL)")
    @Column(name = "login_password", length = 64)
    @FieldMeta
    private String loginPassword;

    @Comment("LOCAL, GOOGLE")
    @Column(name = "provider", nullable = false, length = 20)
    @FieldMeta
    private String provider;

    @Comment("소셜 사용자 고유 ID")
    @Column(name = "provider_id", length = 255)
    @FieldMeta
    private String providerId;

    @Comment("권한")
    @Column(name = "role", columnDefinition = "VARCHAR(32)")
    @Enumerated(EnumType.STRING)
    @FieldMeta
    private AuthRole role;

    @Comment("이름")
    @Column(name = "member_name", length = 64)
    @FieldMeta
    private String memberName;

    @Comment("핸드폰번호")
    @FieldMeta
    @Column(name = "mobile", length = 32)
    private String mobile;

    @Comment("active, suspended, withdrawn")
    @FieldMeta
    @Column(name = "status", length = 32)
    private String status;

    @FieldMeta
    @Comment("탈퇴일")
    @Column(name = "withdrawn_at")
    private LocalDateTime withdrawnAt;

    /**
     * 사용자 등록
     */
    @Builder(builderMethodName = "createMember")
    public Member(String loginId, String loginPassword, AuthRole role, String memberName, String mobile) {
        this.loginId = loginId;
        this.loginPassword = loginPassword;
        this.role = role;
        this.memberName = memberName;
        this.mobile = mobile;
        this.provider = PROVIDER_LOCAL;
        this.status = STATUS_ACTIVE;
    }

    public static final String STATUS_ACTIVE = "active";
    public static final String STATUS_SUSPENDED = "suspended";
    public static final String STATUS_WITHDRAWN = "withdrawn";

    public static final String PROVIDER_LOCAL = "LOCAL";
    public static final String PROVIDER_GOOGLE = "GOOGLE";

    /**
     * 소셜 회원 등록 (비밀번호 없음)
     */
    public static Member createOAuthMember(String email, String memberName, AuthRole role,
                                           String provider, String providerId) {
        Member member = new Member(email, null, role, memberName, null);
        member.provider = provider;
        member.providerId = providerId;
        return member;
    }

    /**
     * 기존 회원에 소셜 계정 연결
     */
    public void linkOAuth(String provider, String providerId) {
        this.provider = provider;
        this.providerId = providerId;
    }

    /**
     * 내 정보 수정 (이름, 전화번호)
     */
    public void updateProfile(String memberName, String mobile) {
        this.memberName = memberName;
        this.mobile = mobile;
    }

    /**
     * 비밀번호 변경 (인코딩된 값)
     */
    public void changePassword(String encodedPassword) {
        this.loginPassword = encodedPassword;
    }

    public boolean hasPassword() {
        return this.loginPassword != null && !this.loginPassword.isBlank();
    }

    /**
     * 관리자 정지 / 해제
     */
    public void changeStatus(String status) {
        this.status = status;
    }

    public boolean isSuspended() {
        return STATUS_SUSPENDED.equals(this.status);
    }

    public boolean isWithdrawn() {
        return this.withdrawnAt != null && !this.withdrawnAt.isAfter(LocalDateTime.now());
    }

}
