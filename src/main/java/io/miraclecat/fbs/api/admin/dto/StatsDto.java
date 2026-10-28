package io.miraclecat.fbs.api.admin.dto;

import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

/**
 * packageName    : io.miraclecat.fbs.api.admin.dto
 * fileName       : StatsDto
 * author         : MiracleCat
 * date           : 2026-10-27
 * description    : 관리자 대시보드 / 통계 DTO
 *                  - 매출 = 결제 승인 금액 - 환불 금액 (결제 승인일 기준)
 *                  - 예약 건수 = 예약 생성일 기준 (취소 포함), 항공사·노선 통계는 확정 예약만
 */
public class StatsDto {

    /**
     * 대시보드
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DashboardRes {
        private long todayBookings;
        private long yesterdayBookings;
        private long monthlySales;
        private long lastMonthSales;          // 지난달 같은 기간(1일 ~ 오늘 날짜)
        private long activeFlights;
        private long todayFlights;            // 오늘 운항(재고 있는) 운항 중 항공편
        private long totalMembers;
        private long newMembersThisMonth;
    }

    /**
     * 기간 통계 요청
     */
    @Getter
    @Setter
    public static class PeriodReq {
        @DateTimeFormat(pattern = "yyyy-MM-dd")
        private LocalDate startDate;
        @DateTimeFormat(pattern = "yyyy-MM-dd")
        private LocalDate endDate;
    }

    /**
     * 기간 통계 응답 (prev*: 바로 이전 같은 길이 기간)
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PeriodRes {
        private LocalDate startDate;
        private LocalDate endDate;
        private long totalBookings;
        private long prevTotalBookings;
        private long sales;
        private long prevSales;
        private long newMembers;
        private long prevNewMembers;
        private double cancelRate;
        private double prevCancelRate;
        private List<DailyCount> dailyBookings;
        private List<NameCount> airlineShares;
        private List<RouteStat> topRoutes;
        private List<MonthSales> monthlySales;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyCount {
        private LocalDate date;
        private long count;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NameCount {
        private String name;
        private long count;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RouteStat {
        private String departureAirport;
        private String departureAirportNameKo;
        private String arrivalAirport;
        private String arrivalAirportNameKo;
        private long count;
        private long revenue;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthSales {
        private String month;   // yyyy-MM
        private long amount;
    }
}
