package io.miraclecat.fbs.api.member.dto;

import io.miraclecat.fbs.core.utils.persistence.PageReq;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * packageName    : io.miraclecat.fbs.api.member.dto
 * fileName       : MemberAdminDto
 * author         : MiracleCat
 * date           : 2026-10-27
 * description    : 관리자 회원 관리 DTO (일반 회원 ROLE_USER 대상)
 *                  status : active(활성) / suspended(정지) / withdrawn(탈퇴, withdrawn_at 기준)
 */
public class MemberAdminDto {

    /**
     * 회원 목록 요청
     */
    @Getter
    @Setter
    public static class SearchReq extends PageReq {
        private String loginId;
        private String memberName;
        private String mobile;
        @DateTimeFormat(pattern = "yyyy-MM-dd")
        private LocalDate joinDate;
        private String status;
    }

    /**
     * 회원 목록 응답
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchRes {
        private Long memberId;
        private String loginId;
        private String memberName;
        private String mobile;
        private String provider;
        private String status;
        private LocalDateTime withdrawnAt;
        private LocalDateTime createdAt;
        private Long bookingCount;
    }

    /**
     * 회원 상세 응답
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DetailRes {
        private Long memberId;
        private String loginId;
        private String memberName;
        private String mobile;
        private String provider;
        private String status;
        private LocalDateTime withdrawnAt;
        private LocalDateTime createdAt;
        private Long bookingCount;
        private Long cancelledCount;
        private List<RecentBooking> recentBookings;
    }

    /**
     * 회원 상세 - 최근 예약
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentBooking {
        private Long id;
        private String bookingNumber;
        private String flightNumber;
        private String departureAirportNameKo;
        private String arrivalAirportNameKo;
        private LocalDate flightDate;
        private Integer passengerCount;
        private Integer totalAmount;
        private String status;
    }

    /**
     * 정지 / 해제 요청 (active / suspended)
     */
    @Getter
    @Setter
    public static class StatusReq {
        private String status;
    }
}
