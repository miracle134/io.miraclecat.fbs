/**
 * packageName    : io.miraclecat.fbs.api.flight.repository
 * fileName       : FlightRepository
 * author         : MiracleCat
 * date           : 2026-05-19
 * description    : 항공편 Repository
 */
package io.miraclecat.fbs.api.flight.domain.repository;

import io.miraclecat.fbs.api.flight.domain.entity.Flight;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FlightRepository extends JpaRepository<Flight, Long> {

    boolean existsByFlightNumber(String flightNumber);

}
