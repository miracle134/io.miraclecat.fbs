package io.miraclecat.fbs.web.controller;

import io.miraclecat.fbs.api.booking.dto.BookingSessionDto;
import io.miraclecat.fbs.api.booking.service.BookingSessionService;
import io.miraclecat.fbs.core.utils.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.ModelAndView;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Optional;

/**
 * packageName    : io.miraclecat.fbs.web.controller
 * fileName       : FlightViewController
 * author         : MiracleCat
 * date           : 2026-09-21
 * description    :
 */

@Slf4j
@Controller
@RequiredArgsConstructor
public class FlightViewController {

    private final BookingSessionService bookingSessionService;

    @Value("${toss.client-key:}")
    private String tossClientKey;

	/**
	 * 항공편 검색 결과
     * 출발지{icn}/목적지{cju}/일자{261003}/인원수
	 */
    @GetMapping("/flight/flight-list/{src}/{dst}/{date}/{counts}")
	public ModelAndView flightListPage(@PathVariable("src") String src,
                                       @PathVariable("dst") String dst,
                                       @PathVariable("date") String date,
                                       @PathVariable("counts") String counts) {

        ModelAndView mv = getModelAndView(src, dst, date, counts);
        if (mv != null) return mv;

        mv = new ModelAndView("/page/member/flight/flight-list");
        mv.addObject("searchValue", Map.of("src", src, "dst", dst, "date", date, "counts", counts));

        return mv;
	}

    /**
	 * 항공편 예약
     * 예약 세션이 없거나 만료·타인 세션이면 진입 불가
	 */
    @GetMapping("/flight/flight-booking/{bookingNo}")
	public ModelAndView flightBookingPage(@PathVariable("bookingNo") String bookingNo) {
        Optional<BookingSessionDto.Session> session =
                bookingSessionService.findOwnedSession(bookingNo, SecurityUtil.getCurrentMemberId());
        if (session.isEmpty()) {
            log.warn("유효하지 않은 예약 세션입니다. sessionId={}", bookingNo);
            return new ModelAndView("redirect:/");
        }

		ModelAndView mv = new ModelAndView("/page/member/flight/flight-booking");
        mv.addObject("bookingNo", bookingNo);
        mv.addObject("tossClientKey", tossClientKey);

		return mv;
	}

    /**
     * flightListPage validation method
     */
    private static ModelAndView getModelAndView(String src, String dst, String date, String counts) {
        ModelAndView mv = new ModelAndView("/error/404");

        // 검색값 검증
        // 1. 출발지 == 목적지
        if (src.equalsIgnoreCase(dst)) {
            log.warn("출발지와 목적지가 같을 수 없습니다.");
            return mv;
        }

        // 2. 과거 일자 (yyMMdd)
        LocalDate searchDate = null;
        try {
            searchDate = LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyyMMdd"));
        } catch (DateTimeParseException e) {
            log.warn("일자 형식이 올바르지 않습니다. (yyyyMMdd)");
            return mv;
        }
        if (searchDate.isBefore(LocalDate.now().plusDays(1))) {
            log.warn("당일 또는 이전 날짜는 선택할 수 없습니다.");
            return mv;
        }

        // 3. 인원수 10명 이상 불가
        int count;
        try {
            count = Integer.parseInt(counts);
        } catch (NumberFormatException e) {
            log.warn("인원수는 숫자여야 합니다.");
            return mv;
        }
        if (count < 1 || count >= 10) {
            log.warn("인원수는 1명 이상 9명 이하만 가능합니다.");
            return mv;
        }
        return null;
    }

}