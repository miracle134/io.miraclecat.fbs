/**
 * packageName    : io.miraclecat.fbs.api.code.repository
 * fileName       : AirportRepository
 * author         : MiracleCat
 * date           : 2026-05-19
 * description    : 공항 Repository
 */
package io.miraclecat.fbs.api.code.domain.repository;

import io.miraclecat.fbs.api.code.domain.entity.Airport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AirportRepository extends JpaRepository<Airport, String> {
}
