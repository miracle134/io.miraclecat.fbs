/**
 * packageName    : io.miraclecat.fbs.api.flight.domain.repository
 * fileName       : InventoryLogRepository
 * author         : MiracleCat
 * date           : 2026-10-25
 * description    : 재고 변경 로그 Repository
 */
package io.miraclecat.fbs.api.flight.domain.repository;

import io.miraclecat.fbs.api.flight.domain.entity.InventoryLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryLogRepository extends JpaRepository<InventoryLog, Long> {

}
