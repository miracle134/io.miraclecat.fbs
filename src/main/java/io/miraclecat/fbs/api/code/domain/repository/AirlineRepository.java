/**
 * packageName    : io.miraclecat.fbs.api.code.repository
 * fileName       : AirlineRepository
 * author         : MiracleCat
 * date           : 2026-05-19
 * description    : 항공사 Repository
 */
package io.miraclecat.fbs.api.code.domain.repository;

import io.miraclecat.fbs.api.code.domain.entity.Airline;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AirlineRepository extends JpaRepository<Airline, String> {
}
