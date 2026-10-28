/**
 * packageName    : io.miraclecat.fbs.api.booking.controller
 * fileName       : BookingRestController
 * author         : MiracleCat
 * date           : 2026-05-19
 * description    : 예약 컨트롤러
 */
package io.miraclecat.fbs.api.booking.controller;

import io.miraclecat.fbs.api.booking.dto.BookingDto;
import io.miraclecat.fbs.api.booking.dto.BookingSessionDto;
import io.miraclecat.fbs.api.booking.service.BookingCancelService;
import io.miraclecat.fbs.api.booking.service.BookingSeatChangeService;
import io.miraclecat.fbs.api.booking.service.BookingService;
import io.miraclecat.fbs.api.booking.service.BookingSessionService;
import io.miraclecat.fbs.core.response.ApiEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
public class BookingRestController {

    private final BookingService bookingService;
    private final BookingSessionService bookingSessionService;
    private final BookingCancelService bookingCancelService;
    private final BookingSeatChangeService bookingSeatChangeService;

    /**
     * 내 예약 목록 조회 (필터·페이징)
     * @param req req
     * @return ApiEntity
     */
    @GetMapping("/api/booking/my")
    public ApiEntity<?> getMyBookings(@ModelAttribute BookingDto.MyReq req) {
        return bookingService.getMyBookings(req);
    }

    /**
     * 예약 세션 생성 (검색 목록 → 예약 화면)
     * @param req req
     * @return ApiEntity
     */
    @PostMapping("/api/booking/session")
    public ApiEntity<?> createSession(@RequestBody BookingSessionDto.CreateReq req) {
        return bookingSessionService.createSession(req);
    }

    /**
     * 예약 세션 조회
     * @param sessionId sessionId
     * @return ApiEntity
     */
    @GetMapping("/api/booking/session/{session-id}")
    public ApiEntity<?> getSession(@PathVariable("session-id") String sessionId) {
        return bookingSessionService.getSession(sessionId);
    }

    /**
     * 예약 세션 삭제
     * @param sessionId sessionId
     * @return ApiEntity
     */
    @DeleteMapping("/api/booking/session/{session-id}")
    public ApiEntity<?> deleteSession(@PathVariable("session-id") String sessionId) {
        return bookingSessionService.deleteSession(sessionId);
    }

    /**
     * 예약 취소
     * @param bookingId bookingId
     * @param req req
     * @return ApiEntity
     */
    @PutMapping("/api/booking/{booking-id}/cancel")
    public ApiEntity<?> cancelBooking(@PathVariable("booking-id") Long bookingId,
                                      @RequestBody(required = false) BookingDto.CancelReq req) {
        return bookingCancelService.cancelByUser(bookingId, req);
    }

    /**
     * 좌석 변경
     * @param bookingId bookingId
     * @param req req
     * @return ApiEntity
     */
    @PostMapping("/api/booking/{booking-id}/seats")
    public ApiEntity<?> changeSeats(@PathVariable("booking-id") Long bookingId,
                                    @RequestBody BookingDto.SeatChangeReq req) {
        return bookingSeatChangeService.changeSeats(bookingId, req);
    }

    /**
     * 예약 목록 (관리자)
     * @param req req
     * @return ApiEntity
     */
    @GetMapping("/api/admin/booking")
    public ApiEntity<?> getAdminBookings(@ModelAttribute BookingDto.AdminSearchReq req) {
        return bookingService.getAdminBookings(req);
    }

    /**
     * 예약 상세 (관리자, 탑승객·이력 포함)
     * @param bookingId bookingId
     * @return ApiEntity
     */
    @GetMapping("/api/admin/booking/{booking-id}")
    public ApiEntity<?> getAdminBookingDetail(@PathVariable("booking-id") Long bookingId) {
        return bookingService.getAdminBookingDetail(bookingId);
    }

    /**
     * 예약 취소 (관리자, 전액/항공권 금액 환불 선택)
     * @param bookingId bookingId
     * @param req req
     * @return ApiEntity
     */
    @PutMapping("/api/admin/booking/{booking-id}/cancel")
    public ApiEntity<?> cancelBookingByAdmin(@PathVariable("booking-id") Long bookingId,
                                             @RequestBody BookingDto.AdminCancelReq req) {
        return bookingCancelService.cancelByAdmin(bookingId, req);
    }

}
