/**
 * packageName    : io.miraclecat.fbs.api.flight.domain.entity
 * fileName       : SeatReservation
 * author         : MiracleCat
 * date           : 2026-10-25
 * description    : 좌석 확정 (seat_reservations)
 *                  - UNIQUE(inventory_id, seat_number) 로 좌석 중복 최종 방어
 *                  - 예약 확정 시 생성, 취소/좌석변경 시 삭제 (수정 없음)
 */
package io.miraclecat.fbs.api.flight.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "seat_reservations",
        uniqueConstraints = @UniqueConstraint(name = "uk_inventory_seat", columnNames = {"inventory_id", "seat_number"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SeatReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "inventory_id", nullable = false)
    private Long inventoryId;

    @Column(name = "seat_number", nullable = false, length = 5)
    private String seatNumber;

    @Column(name = "booking_id", nullable = false)
    private Long bookingId;

    @Column(name = "booking_passenger_id", nullable = false)
    private Long bookingPassengerId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public SeatReservation(Long inventoryId, String seatNumber, Long bookingId, Long bookingPassengerId) {
        this.inventoryId = inventoryId;
        this.seatNumber = seatNumber;
        this.bookingId = bookingId;
        this.bookingPassengerId = bookingPassengerId;
        this.createdAt = LocalDateTime.now();
    }

}