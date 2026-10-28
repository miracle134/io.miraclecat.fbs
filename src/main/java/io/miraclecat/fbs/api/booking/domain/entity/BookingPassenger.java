/**
 * packageName    : io.miraclecat.fbs.api.booking.entity
 * fileName       : BookingPassenger
 * author         : MiracleCat
 * date           : 2026-05-19
 * description    : 탑승객 엔티티
 */
package io.miraclecat.fbs.api.booking.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "booking_passengers")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BookingPassenger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_id", nullable = false)
    private Long bookingId;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "name_en", nullable = false, length = 50)
    private String nameEn;

    @Column(name = "email", nullable = false, length = 64)
    private String email;

    @Column(name = "mobile", length = 32)
    private String mobile;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Column(name = "gender", nullable = false, length = 10)
    private String gender;

    @Column(name = "seat_number", length = 5)
    private String seatNumber;

    @Column(name = "created_id")
    private Long createdId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_id")
    private Long updatedId;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public BookingPassenger(Long bookingId, String name, String nameEn, LocalDate birthDate,
                            String email, String mobile,
                            String gender, String seatNumber, Long createdId) {
        this.bookingId = bookingId;
        this.name = name;
        this.nameEn = nameEn;
        this.email = email;
        this.mobile = mobile;
        this.birthDate = birthDate;
        this.gender = gender;
        this.seatNumber = seatNumber;
        this.createdId = createdId;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void changeSeat(String seatNumber, Long updatedId) {
        this.seatNumber = seatNumber;
        this.updatedId = updatedId;
        this.updatedAt = LocalDateTime.now();
    }

}
