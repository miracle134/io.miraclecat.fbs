package io.miraclecat.fbs.api.member.domain.repository;

import io.miraclecat.fbs.api.member.domain.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * packageName    : io.miraclecat.fbs.domain.member
 * fileName       : MemberRepository
 * author         : MiracleCat 
 * date           : 2023-07-07
 * description    :
 */
public interface MemberRepository extends JpaRepository<Member, Long> {

    /**
     * 기본 로그인
     * @param loginId 아이디
     * @return 회원 정보
     */
    Optional<Member> findByLoginId(String loginId);

    /**
     * 활성화여부, 사용여부 로그인
     * @param loginId 아이디
     * @return 회원 정보
     */
    @Query("SELECT m FROM Member m " +
           "WHERE m.loginId = :loginId " +
           "AND (m.withdrawnAt IS NULL OR m.withdrawnAt > CURRENT_DATE)")
    Optional<Member> findByLoginIdAndWithdrawnAt(@Param("loginId") String loginId);

    /**
     * @param loginId 아이디
     * @return 아이디 여부
     */
    boolean existsByLoginId(String loginId);

    /**
     * 소셜 로그인 회원 조회
     */
    Optional<Member> findByProviderAndProviderId(String provider, String providerId);

}
