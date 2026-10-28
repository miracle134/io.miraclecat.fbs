/**
 * packageName    : io.miraclecat.fbs.api.flight.domain.repository
 * fileName       : SeatReservationRepository
 * author         : MiracleCat
 * date           : 2026-10-25
 * description    : 좌석 확정 Repository
 *                  - 삭제는 벌크 JPQL 사용 (아래 주석 참고)
 *                  - 좌석 목록 조회 등 복합 조회는 FlightDao(QueryDSL)에서 처리
 */
package io.miraclecat.fbs.api.flight.domain.repository;

import io.miraclecat.fbs.api.flight.domain.entity.SeatReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface SeatReservationRepository extends JpaRepository<SeatReservation, Long> {

    /**
     * 재고(항공편+운항일)의 예약 확정 좌석 번호 목록
     */
    @Query("select s.seatNumber from SeatReservation s where s.inventoryId = :inventoryId")
    List<String> findSeatNumbersByInventoryId(@Param("inventoryId") Long inventoryId);

    boolean existsByInventoryIdAndSeatNumber(Long inventoryId, String seatNumber);

    /**
     * 재고의 예약 확정 좌석 수
     */
    long countByInventoryId(Long inventoryId);

    @Query("select s.seatNumber from SeatReservation s where s.bookingId = :bookingId")
    List<String> findSeatNumbersByBookingId(@Param("bookingId") Long bookingId);

    /**
     * 예약 취소 시 해당 예약의 좌석 전체 삭제
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from SeatReservation s where s.bookingId = :bookingId")
    int deleteByBookingId(@Param("bookingId") Long bookingId);

    /**
     * 좌석 변경 시 대상 탑승객들의 기존 좌석 삭제
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from SeatReservation s where s.bookingPassengerId in :passengerIds")
    int deleteByBookingPassengerIds(@Param("passengerIds") Collection<Long> passengerIds);

}
