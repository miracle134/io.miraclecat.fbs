package io.miraclecat.fbs.api.admin.service;

import io.miraclecat.fbs.api.admin.dao.StatsDao;
import io.miraclecat.fbs.api.admin.dto.StatsDto;
import io.miraclecat.fbs.api.booking.domain.entity.Booking;
import io.miraclecat.fbs.core.config.ReadTx;
import io.miraclecat.fbs.core.response.ApiEntity;
import io.miraclecat.fbs.core.response.HttpsStatusCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static io.miraclecat.fbs.core.exception.GlobalException.business;

/**
 * packageName    : io.miraclecat.fbs.api.admin.service
 * fileName       : AdminStatsService
 * author         : MiracleCat
 * date           : 2026-10-27
 * description    : 관리자 대시보드 / 기간 통계
 */
@Service
@RequiredArgsConstructor
public class AdminStatsService {

    private static final int MAX_PERIOD_DAYS = 366;
    private static final int TOP_ROUTES = 10;
    private static final int MONTHS = 6;

    private final StatsDao statsDao;

    /**
     * 대시보드 요약
     */
    @ReadTx
    public ApiEntity<StatsDto.DashboardRes> getDashboard() {
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime tomorrowStart = today.plusDays(1).atStartOfDay();
        LocalDateTime monthStart = today.withDayOfMonth(1).atStartOfDay();

        // 지난달 같은 기간: 지난달 1일 ~ 지난달의 오늘 날짜(말일 보정)
        YearMonth lastMonth = YearMonth.from(today).minusMonths(1);
        LocalDate lastMonthSameDay = lastMonth.atDay(Math.min(today.getDayOfMonth(), lastMonth.lengthOfMonth()));

        return ApiEntity.of(HttpsStatusCode.SUCCESS, StatsDto.DashboardRes.builder()
                .todayBookings(statsDao.countBookings(todayStart, tomorrowStart, null))
                .yesterdayBookings(statsDao.countBookings(today.minusDays(1).atStartOfDay(), todayStart, null))
                .monthlySales(statsDao.sumNetSales(monthStart, tomorrowStart))
                .lastMonthSales(statsDao.sumNetSales(lastMonth.atDay(1).atStartOfDay(), lastMonthSameDay.plusDays(1).atStartOfDay()))
                .activeFlights(statsDao.countActiveFlights())
                .todayFlights(statsDao.countFlightsOn(today))
                .totalMembers(statsDao.countMembers())
                .newMembersThisMonth(statsDao.countNewMembers(monthStart, tomorrowStart))
                .build());
    }

    /**
     * 기간 통계 (이전 같은 길이 기간과 비교)
     */
    @ReadTx
    public ApiEntity<StatsDto.PeriodRes> getPeriodStats(StatsDto.PeriodReq req) {
        LocalDate end = req.getEndDate() != null ? req.getEndDate() : LocalDate.now();
        LocalDate start = req.getStartDate() != null ? req.getStartDate() : end.minusDays(29);
        if (start.isAfter(end)) {
            throw business("시작일이 종료일보다 늦습니다.", HttpsStatusCode.INVALID_PARAMETER);
        }
        long days = ChronoUnit.DAYS.between(start, end) + 1;
        if (days > MAX_PERIOD_DAYS) {
            throw business("조회 기간은 최대 1년입니다.", HttpsStatusCode.INVALID_PARAMETER);
        }

        LocalDateTime from = start.atStartOfDay();
        LocalDateTime to = end.plusDays(1).atStartOfDay();
        LocalDateTime prevFrom = start.minusDays(days).atStartOfDay();

        long total = statsDao.countBookings(from, to, null);
        long cancelled = statsDao.countBookings(from, to, Booking.STATUS_CANCELLED);
        long prevTotal = statsDao.countBookings(prevFrom, from, null);
        long prevCancelled = statsDao.countBookings(prevFrom, from, Booking.STATUS_CANCELLED);

        YearMonth endMonth = YearMonth.from(end);
        YearMonth startMonth = endMonth.minusMonths(MONTHS - 1);

        return ApiEntity.of(HttpsStatusCode.SUCCESS, StatsDto.PeriodRes.builder()
                .startDate(start)
                .endDate(end)
                .totalBookings(total)
                .prevTotalBookings(prevTotal)
                .sales(statsDao.sumNetSales(from, to))
                .prevSales(statsDao.sumNetSales(prevFrom, from))
                .newMembers(statsDao.countNewMembers(from, to))
                .prevNewMembers(statsDao.countNewMembers(prevFrom, from))
                .cancelRate(rate(cancelled, total))
                .prevCancelRate(rate(prevCancelled, prevTotal))
                .dailyBookings(fillDays(statsDao.dailyBookings(from, to), start, end))
                .airlineShares(statsDao.airlineShares(from, to))
                .topRoutes(statsDao.topRoutes(from, to, TOP_ROUTES))
                .monthlySales(fillMonths(statsDao.monthlySales(
                        startMonth.atDay(1).atStartOfDay(), endMonth.plusMonths(1).atDay(1).atStartOfDay()),
                        startMonth, endMonth))
                .build());
    }

    private double rate(long part, long total) {
        return total == 0 ? 0.0 : Math.round(part * 1000.0 / total) / 10.0;
    }

    /**
     * 예약이 없는 날짜는 0건으로 채움
     */
    private List<StatsDto.DailyCount> fillDays(List<StatsDto.DailyCount> rows, LocalDate start, LocalDate end) {
        Map<LocalDate, Long> byDate = rows.stream()
                .collect(Collectors.toMap(StatsDto.DailyCount::getDate, StatsDto.DailyCount::getCount, Long::sum));
        return start.datesUntil(end.plusDays(1))
                .map(d -> StatsDto.DailyCount.builder().date(d).count(byDate.getOrDefault(d, 0L)).build())
                .toList();
    }

    /**
     * 매출이 없는 달은 0원으로 채움
     */
    private List<StatsDto.MonthSales> fillMonths(List<StatsDto.MonthSales> rows, YearMonth start, YearMonth end) {
        Map<String, StatsDto.MonthSales> byMonth = rows.stream()
                .collect(Collectors.toMap(StatsDto.MonthSales::getMonth, Function.identity(), (a, b) -> a));
        List<StatsDto.MonthSales> result = new ArrayList<>();
        for (YearMonth m = start; !m.isAfter(end); m = m.plusMonths(1)) {
            String key = m.toString();
            result.add(byMonth.getOrDefault(key, StatsDto.MonthSales.builder().month(key).amount(0).build()));
        }
        return result;
    }
}
