/**
 * packageName    : io.miraclecat.fbs.api.flight.entity
 * fileName       : FlightInventory
 * author         : MiracleCat
 * date           : 2026-05-19
 * description    : 항공편 재고 엔티티
 */
package io.miraclecat.fbs.api.flight.domain.entity;

import io.miraclecat.fbs.api.flight.dto.FlightInventoryDto;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "flight_inventories",
        uniqueConstraints = @UniqueConstraint(name = "uk_flight_date", columnNames = {"flight_id", "flight_date"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FlightInventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "flight_id", nullable = false)
    private Long flightId;

    @Column(name = "flight_date", nullable = false)
    private LocalDate flightDate;

    @Column(name = "available_seats", nullable = false)
    private Integer availableSeats;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public FlightInventory(Long flightId, LocalDate flightDate, Integer availableSeats) {
        this.flightId = flightId;
        this.flightDate = flightDate;
        this.availableSeats = availableSeats;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void adjustSeats(FlightInventoryDto.AdjustReq req) {
        this.availableSeats = req.getAvailableSeats();
        this.updatedAt = LocalDateTime.now();
    }

    // 예약/취소의 좌석 수 변경은 FlightInventoryRepository 원자적 UPDATE(decreaseSeats / increaseSeats)로 처리

}
