/**
 * packageName    : io.miraclecat.fbs.api.flight.repository
 * fileName       : FlightInventoryRepository
 * author         : MiracleCat
 * date           : 2026-05-19
 * description    : 항공편 재고 Repository
 *                  - 예약/취소의 좌석 수 변경은 원자적 UPDATE (조회 후 수정이 아니라 DB가 한 문장으로 차감)
 *                    → 같은 재고에 동시 결제가 몰려도 충돌 없이 순서대로 반영, 부족하면 0건 갱신
 *                  - versioned: 관리자 수동 조정(@Version 엔티티 수정)과 겹치면 조정 쪽이 충돌로 감지
 */
package io.miraclecat.fbs.api.flight.domain.repository;

import io.miraclecat.fbs.api.flight.domain.entity.FlightInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

public interface FlightInventoryRepository extends JpaRepository<FlightInventory, Long> {

    Optional<FlightInventory> findByFlightIdAndFlightDate(Long flightId, LocalDate flightDate);

    boolean existsByFlightId(Long flightId);

    /**
     * 좌석 차감 (남은 좌석이 count 이상일 때만) — 갱신 건수 0이면 좌석 부족
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update versioned FlightInventory i " +
            "set i.availableSeats = i.availableSeats - :count, i.updatedAt = :now " +
            "where i.id = :id and i.availableSeats >= :count")
    int decreaseSeats(@Param("id") Long id, @Param("count") int count, @Param("now") LocalDateTime now);

    /**
     * 좌석 복원 (예약 취소)
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update versioned FlightInventory i " +
            "set i.availableSeats = i.availableSeats + :count, i.updatedAt = :now " +
            "where i.id = :id")
    int increaseSeats(@Param("id") Long id, @Param("count") int count, @Param("now") LocalDateTime now);

    /**
     * 현재 남은 좌석 (원자적 UPDATE 직후 같은 트랜잭션에서 조회 → 행 잠금 중이라 정확한 값)
     */
    @Query("select i.availableSeats from FlightInventory i where i.id = :id")
    Integer findAvailableSeats(@Param("id") Long id);

}
