/**
 * packageName    : io.miraclecat.fbs.api.booking.entity
 * fileName       : BookingHistory
 * author         : MiracleCat
 * date           : 2026-05-19
 * description    : 예약 이력 엔티티
 */
package io.miraclecat.fbs.api.booking.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "booking_histories")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BookingHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_id", nullable = false)
    private Long bookingId;

    @Column(name = "previous_status", length = 20)
    private String previousStatus;

    @Column(name = "current_status", nullable = false, length = 20)
    private String currentStatus;

    @Column(name = "changed_by", nullable = false, length = 20)
    private String changedBy;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public BookingHistory(Long bookingId, String previousStatus, String currentStatus,
                          String changedBy, String reason) {
        this.bookingId = bookingId;
        this.previousStatus = previousStatus;
        this.currentStatus = currentStatus;
        this.changedBy = changedBy;
        this.reason = reason;
        this.createdAt = LocalDateTime.now();
    }

}
