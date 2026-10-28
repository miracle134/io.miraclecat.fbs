/**
 * packageName    : io.miraclecat.fbs.api.flight.domain.entity
 * fileName       : InventoryLog
 * author         : MiracleCat
 * date           : 2026-10-25
 * description    : 재고 변경 로그 엔티티
 */
package io.miraclecat.fbs.api.flight.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "inventory_logs")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InventoryLog {

    public static final String TYPE_RESERVE = "RESERVE";
    public static final String TYPE_RELEASE = "RELEASE";
    public static final String TYPE_ADJUST = "ADJUST";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "flight_id", nullable = false)
    private Long flightId;

    @Column(name = "flight_date", nullable = false)
    private LocalDate flightDate;

    @Column(name = "change_type", nullable = false, length = 20)
    private String changeType;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "before_seats", nullable = false)
    private Integer beforeSeats;

    @Column(name = "after_seats", nullable = false)
    private Integer afterSeats;

    @Column(name = "booking_id")
    private Long bookingId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public InventoryLog(Long flightId, LocalDate flightDate, String changeType,
                        Integer beforeSeats, Integer afterSeats, Long bookingId) {
        this.flightId = flightId;
        this.flightDate = flightDate;
        this.changeType = changeType;
        this.beforeSeats = beforeSeats;
        this.afterSeats = afterSeats;
        this.quantity = afterSeats - beforeSeats;
        this.bookingId = bookingId;
        this.createdAt = LocalDateTime.now();
    }

}
