/**
 * packageName    : io.miraclecat.fbs.api.admin.repository
 * fileName       : SchedulerLogRepository
 * author         : MiracleCat
 * date           : 2026-05-19
 * description    : 스케줄러 로그 Repository
 */
package io.miraclecat.fbs.api.admin.domain.repository;

import io.miraclecat.fbs.api.admin.domain.entity.SchedulerLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SchedulerLogRepository extends JpaRepository<SchedulerLog, Long> {
}
