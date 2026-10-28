/**
 * packageName    : io.miraclecat.fbs.api.booking.repository
 * fileName       : BookingRepository
 * author         : MiracleCat
 * date           : 2026-05-19
 * description    : 예약 Repository
 */
package io.miraclecat.fbs.api.booking.domain.repository;

import io.miraclecat.fbs.api.booking.domain.entity.Booking;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByBookingNumber(String bookingNumber);

    /**
     * 예약 행 비관적 락 (SELECT ... FOR UPDATE) — 취소·좌석 변경 동시 실행 방지
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Booking b where b.id = :id")
    Optional<Booking> findByIdForUpdate(@Param("id") Long id);

}
