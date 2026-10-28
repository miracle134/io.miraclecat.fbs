/**
 * packageName    : io.miraclecat.fbs.api.booking.repository
 * fileName       : BookingPassengerRepository
 * author         : MiracleCat
 * date           : 2026-05-19
 * description    : 탑승객 Repository
 */
package io.miraclecat.fbs.api.booking.domain.repository;

import io.miraclecat.fbs.api.booking.domain.entity.BookingPassenger;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookingPassengerRepository extends JpaRepository<BookingPassenger, Long> {

    List<BookingPassenger> findByBookingIdOrderByIdAsc(Long bookingId);

}
