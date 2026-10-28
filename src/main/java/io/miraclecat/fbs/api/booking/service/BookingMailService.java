package io.miraclecat.fbs.api.booking.service;

import io.miraclecat.fbs.api.booking.domain.entity.Booking;
import io.miraclecat.fbs.api.booking.dto.BookingSessionDto;
import io.miraclecat.fbs.api.flight.domain.entity.Flight;
import io.miraclecat.fbs.api.flight.domain.repository.FlightRepository;
import io.miraclecat.fbs.core.mail.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * packageName    : io.miraclecat.fbs.api.booking.service
 * fileName       : BookingMailService
 * author         : MiracleCat
 * date           : 2026-10-27
 * description    : 예약 완료 메일 (탑승객1 이메일로 발송)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookingMailService {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final FlightRepository flightRepository;
    private final MailService mailService;

    /**
     * 예약 완료 메일 (실패해도 예외를 던지지 않음)
     */
    public void sendBookingConfirmed(Booking booking, BookingSessionDto.Session session) {
        try {
            List<BookingSessionDto.Passenger> passengers = session.getPassengers();
            if (passengers == null || passengers.isEmpty()) return;
            String to = passengers.get(0).getEmail();

            Flight flight = flightRepository.findById(session.getFlightId()).orElse(null);
            NumberFormat won = NumberFormat.getNumberInstance(Locale.KOREA);

            Map<String, Object> vars = new HashMap<>();
            vars.put("bookingNumber", booking.getBookingNumber());
            vars.put("flightNumber", flight != null ? flight.getFlightNumber() : "-");
            vars.put("flightDate", session.getFlightDate());
            vars.put("departureAirport", session.getDepartureAirport());
            vars.put("arrivalAirport", session.getArrivalAirport());
            vars.put("departureTime", flight != null ? flight.getDepartureTime().format(TIME) : "-");
            vars.put("arrivalTime", flight != null ? flight.getArrivalTime().format(TIME) : "-");
            vars.put("passengers", passengers);
            vars.put("fareAmount", won.format(session.getFareAmount()));
            vars.put("bookingFeeAmount", won.format(session.getBookingFeeAmount()));
            vars.put("totalAmount", won.format(session.getTotalAmount()));

            mailService.send(to, "[Flight Booking] 예약이 완료되었습니다 (" + booking.getBookingNumber() + ")",
                    "booking-confirmed", vars);
        } catch (Exception e) {
            log.warn("[예약 완료 메일 준비 실패] bookingNumber={}, cause={}", booking.getBookingNumber(), e.getMessage());
        }
    }
}
