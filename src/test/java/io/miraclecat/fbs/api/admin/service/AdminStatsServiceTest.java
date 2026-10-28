package io.miraclecat.fbs.api.admin.service;

import io.miraclecat.fbs.api.admin.dao.StatsDao;
import io.miraclecat.fbs.api.admin.dto.StatsDto;
import io.miraclecat.fbs.api.booking.domain.entity.Booking;
import io.miraclecat.fbs.core.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 기간 통계 — 이전 기간 계산, 취소율, 빈 날짜/월 채우기, 기간 검증
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminStatsServiceTest {

    private static final LocalDate START = LocalDate.of(2026, 10, 1);
    private static final LocalDate END = LocalDate.of(2026, 10, 7);

    @Mock StatsDao statsDao;
    @InjectMocks AdminStatsService adminStatsService;

    @Test
    @DisplayName("7일 조회 → 이전 기간은 바로 앞 7일, 예약이 없는 날은 0건으로 채움")
    void periodStats_fillsMissingDaysAndComparesPreviousPeriod() {
        LocalDateTime from = START.atStartOfDay();
        LocalDateTime to = END.plusDays(1).atStartOfDay();
        LocalDateTime prevFrom = START.minusDays(7).atStartOfDay();

        when(statsDao.countBookings(from, to, null)).thenReturn(40L);
        when(statsDao.countBookings(from, to, Booking.STATUS_CANCELLED)).thenReturn(3L);
        when(statsDao.countBookings(prevFrom, from, null)).thenReturn(20L);
        when(statsDao.countBookings(prevFrom, from, Booking.STATUS_CANCELLED)).thenReturn(0L);
        when(statsDao.dailyBookings(from, to)).thenReturn(List.of(
                daily(START, 10), daily(START.plusDays(3), 30)));
        when(statsDao.monthlySales(any(), any())).thenReturn(List.of(
                StatsDto.MonthSales.builder().month("2026-09").amount(1_000_000).build()));

        StatsDto.PeriodRes res = req(START, END);

        assertThat(res.getTotalBookings()).isEqualTo(40);
        assertThat(res.getPrevTotalBookings()).isEqualTo(20);
        assertThat(res.getCancelRate()).isEqualTo(7.5);     // 3 / 40
        assertThat(res.getPrevCancelRate()).isEqualTo(0.0);

        assertThat(res.getDailyBookings()).hasSize(7);
        assertThat(res.getDailyBookings()).extracting(StatsDto.DailyCount::getCount)
                .containsExactly(10L, 0L, 0L, 30L, 0L, 0L, 0L);

        // 종료월 기준 최근 6개월, 매출 없는 달은 0
        assertThat(res.getMonthlySales()).extracting(StatsDto.MonthSales::getMonth)
                .containsExactly("2026-05", "2026-06", "2026-07", "2026-08", "2026-09", "2026-10");
        assertThat(res.getMonthlySales()).extracting(StatsDto.MonthSales::getAmount)
                .containsExactly(0L, 0L, 0L, 0L, 1_000_000L, 0L);

        verify(statsDao).countBookings(prevFrom, from, null);
    }

    @Test
    @DisplayName("예약 0건이면 취소율 0 (0으로 나누지 않음)")
    void noBookings_cancelRateZero() {
        when(statsDao.countBookings(any(), any(), any())).thenReturn(0L);
        when(statsDao.dailyBookings(any(), any())).thenReturn(List.of());
        when(statsDao.monthlySales(any(), any())).thenReturn(List.of());

        StatsDto.PeriodRes res = req(START, END);

        assertThat(res.getCancelRate()).isEqualTo(0.0);
        assertThat(res.getDailyBookings()).allMatch(d -> d.getCount() == 0);
    }

    @Test
    @DisplayName("시작일이 종료일보다 늦거나 1년을 넘으면 거부")
    void invalidPeriod_rejected() {
        assertThatThrownBy(() -> req(END, START)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> req(START, START.plusDays(366))).isInstanceOf(BusinessException.class);
        verifyNoInteractions(statsDao);
    }

    private StatsDto.PeriodRes req(LocalDate start, LocalDate end) {
        StatsDto.PeriodReq req = new StatsDto.PeriodReq();
        req.setStartDate(start);
        req.setEndDate(end);
        return adminStatsService.getPeriodStats(req).getBody().getResult();
    }

    private StatsDto.DailyCount daily(LocalDate date, long count) {
        return StatsDto.DailyCount.builder().date(date).count(count).build();
    }
}
