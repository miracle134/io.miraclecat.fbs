/**
 * packageName    : io.miraclecat.fbs.api.booking.repository
 * fileName       : BookingHistoryRepository
 * author         : MiracleCat
 * date           : 2026-05-19
 * description    : 예약 이력 Repository
 */
package io.miraclecat.fbs.api.booking.domain.repository;

import io.miraclecat.fbs.api.booking.domain.entity.BookingHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingHistoryRepository extends JpaRepository<BookingHistory, Long> {
}
