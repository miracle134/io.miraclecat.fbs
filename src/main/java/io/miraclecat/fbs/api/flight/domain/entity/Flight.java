/**
 * packageName    : io.miraclecat.fbs.api.flight.entity
 * fileName       : Flight
 * author         : MiracleCat
 * date           : 2026-05-19
 * description    : 항공편 엔티티
 */
package io.miraclecat.fbs.api.flight.domain.entity;

import io.miraclecat.fbs.api.flight.dto.FlightDto;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Entity
@Table(name = "flights")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Flight {

    public static final String STATUS_ACTIVE = "active";
    public static final String STATUS_INACTIVE = "inactive";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "flight_number", nullable = false, unique = true, length = 20)
    private String flightNumber;

    @Column(name = "airline_code", nullable = false, length = 2)
    private String airlineCode;

    @Column(name = "departure_airport", nullable = false, length = 3)
    private String departureAirport;

    @Column(name = "arrival_airport", nullable = false, length = 3)
    private String arrivalAirport;

    @Column(name = "departure_time", nullable = false)
    private LocalTime departureTime;

    @Column(name = "arrival_time", nullable = false)
    private LocalTime arrivalTime;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    @Column(name = "total_seats", nullable = false)
    private Integer totalSeats;

    @Column(name = "base_price", nullable = false)
    private Integer basePrice;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public Flight(String flightNumber, String airlineCode, String departureAirport,
                  String arrivalAirport, LocalTime departureTime, LocalTime arrivalTime,
                  Integer durationMinutes, Integer totalSeats, Integer basePrice, String status) {
        this.flightNumber = flightNumber;
        this.airlineCode = airlineCode;
        this.departureAirport = departureAirport;
        this.arrivalAirport = arrivalAirport;
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
        this.durationMinutes = durationMinutes;
        this.totalSeats = totalSeats;
        this.basePrice = basePrice;
        this.status = status != null ? status : STATUS_ACTIVE;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void updateFlight(FlightDto.UpdateReq req) {
        this.airlineCode = req.getAirlineCode();
        this.departureAirport = req.getDepartureAirport();
        this.arrivalAirport = req.getArrivalAirport();
        this.departureTime = req.getDepartureTime();
        this.arrivalTime = req.getArrivalTime();
        this.durationMinutes = req.getDurationMinutes();
        this.totalSeats = req.getTotalSeats();
        this.basePrice = req.getBasePrice();
        this.status = req.getStatus();
        this.updatedAt = LocalDateTime.now();
    }

    public void deleteFlight() {
        this.status = STATUS_INACTIVE;
        this.updatedAt = LocalDateTime.now();
    }

}
