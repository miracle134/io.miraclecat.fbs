/**
 * packageName    : io.miraclecat.fbs.api.booking.service
 * fileName       : BookingService
 * author         : MiracleCat
 * date           : 2026-05-19
 * description    : 예약 조회 서비스 (내 예약 / 관리자 예약 관리)
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 2026-05-19        MiracleCat         최초 생성
 * 2026-10-27        MiracleCat         권한 없는 전체 조회 API 제거, 관리자 조회 추가
 */
package io.miraclecat.fbs.api.booking.service;

import io.miraclecat.fbs.api.booking.dao.BookingDao;
import io.miraclecat.fbs.api.booking.domain.entity.Booking;
import io.miraclecat.fbs.api.booking.dto.BookingDto;
import io.miraclecat.fbs.api.booking.dto.BookingHistoryDto;
import io.miraclecat.fbs.core.config.ReadTx;
import io.miraclecat.fbs.core.response.ApiEntity;
import io.miraclecat.fbs.core.response.HttpsStatusCode;
import io.miraclecat.fbs.core.utils.persistence.PageRes;
import io.miraclecat.fbs.core.utils.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.miraclecat.fbs.core.exception.GlobalException.business;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingDao bookingDao;

    /**
     * 내 예약 목록 조회 (필터·페이징, 탑승객 포함)
     */
    @ReadTx
    public ApiEntity<PageRes<BookingDto.MyRes>> getMyBookings(BookingDto.MyReq req) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        if (memberId == null) {
            throw business(HttpsStatusCode.LOGIN_REQUIRED);
        }

        PageRes<BookingDto.MyRes> page = bookingDao.findMyBookings(req, memberId);
        List<Long> bookingIds = page.getContent().stream().map(BookingDto.MyRes::getId).toList();
        Map<Long, List<BookingDto.MyPassenger>> passengers = bookingDao.findMyPassengers(bookingIds).stream()
                .collect(Collectors.groupingBy(BookingDto.MyPassenger::getBookingId));

        LocalDate today = LocalDate.now();
        page.getContent().forEach(b -> {
            b.setPassengers(passengers.getOrDefault(b.getId(), List.of()));
            b.setChangeable(Booking.STATUS_CONFIRMED.equals(b.getStatus()) && today.isBefore(b.getFlightDate()));
        });
        return ApiEntity.of(HttpsStatusCode.SUCCESS, page);
    }

    /**
     * 예약 목록 (관리자)
     */
    @ReadTx
    public ApiEntity<PageRes<BookingDto.AdminSearchRes>> getAdminBookings(BookingDto.AdminSearchReq req) {
        if (req.getStartDate() != null && req.getEndDate() != null && req.getStartDate().isAfter(req.getEndDate())) {
            throw business("시작일이 종료일보다 늦습니다.", HttpsStatusCode.INVALID_PARAMETER);
        }
        return ApiEntity.of(HttpsStatusCode.SUCCESS, bookingDao.findAdminBookings(req));
    }

    /**
     * 예약 상세 (관리자, 탑승객·이력 포함)
     */
    @ReadTx
    public ApiEntity<BookingDto.AdminDetailRes> getAdminBookingDetail(Long bookingId) {
        BookingDto.AdminDetailRes detail = bookingDao.findAdminBookingDetail(bookingId);
        if (detail == null) throw business(HttpsStatusCode.RESOURCE_NOT_FOUND);

        BookingHistoryDto.SearchReq historyReq = new BookingHistoryDto.SearchReq();
        historyReq.setBookingId(bookingId);

        detail.setPassengers(bookingDao.findAdminPassengers(bookingId));
        detail.setHistories(bookingDao.findBookingHistories(historyReq));
        return ApiEntity.of(HttpsStatusCode.SUCCESS, detail);
    }

}
