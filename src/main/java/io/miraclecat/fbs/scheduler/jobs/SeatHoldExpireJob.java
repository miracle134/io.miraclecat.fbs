/**
 * packageName    : io.miraclecat.fbs.scheduler.jobs
 * fileName       : SeatHoldExpireJob
 * author         : MiracleCat
 * date           : 2026-10-25
 * description    : 좌석 임시 선점 만료 정리 + RELEASED 전파
 *                  - Redis TTL 만료는 알림이 보장되지 않으므로 주기적으로 만료 좌석을 찾아 정리·전파
 *                  - ShedLock으로 다중 서버에서 한 대만 실행
 *                  - 5초 주기라 scheduler_logs는 처리 건이 있거나 실패한 경우만 기록
 */
package io.miraclecat.fbs.scheduler.jobs;

import io.miraclecat.fbs.api.admin.domain.entity.SchedulerLog;
import io.miraclecat.fbs.api.admin.domain.repository.SchedulerLogRepository;
import io.miraclecat.fbs.api.flight.dto.SeatDto;
import io.miraclecat.fbs.api.flight.redis.SeatHoldStore;
import io.miraclecat.fbs.api.flight.websocket.SeatEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SeatHoldExpireJob {

    private static final String JOB_NAME = "SeatHoldExpireJob";

    private final SeatHoldStore seatHoldStore;
    private final SeatEventPublisher seatEventPublisher;
    private final SchedulerLogRepository schedulerLogRepository;

    @Scheduled(fixedDelay = 5000)
    @SchedulerLock(name = JOB_NAME, lockAtMostFor = "PT30S", lockAtLeastFor = "PT3S")
    public void execute() {
        LocalDateTime startedAt = LocalDateTime.now();
        int processedCount = 0;
        StringBuilder errors = new StringBuilder();

        for (Long inventoryId : seatHoldStore.findHoldInventoryIds()) {
            try {
                List<String> released = seatHoldStore.releaseExpired(inventoryId);
                if (released.isEmpty()) continue;

                seatEventPublisher.publish(SeatDto.EventType.RELEASED, inventoryId, released, null);
                processedCount += released.size();
                log.debug("[{}] inventoryId={}, released={}", JOB_NAME, inventoryId, released);
            } catch (Exception e) {
                log.error("[{}] inventoryId={} 처리 실패 - {}", JOB_NAME, inventoryId, e.getMessage(), e);
                errors.append("inventoryId=").append(inventoryId).append(": ").append(e.getMessage()).append('\n');
            }
        }

        if (processedCount > 0 || !errors.isEmpty()) {
            saveSchedulerLog(startedAt, processedCount, errors.isEmpty() ? null : errors.toString());
        }
    }

    private void saveSchedulerLog(LocalDateTime startedAt, int processedCount, String errorMessage) {
        LocalDateTime finishedAt = LocalDateTime.now();
        try {
            schedulerLogRepository.save(SchedulerLog.builder()
                    .jobName(JOB_NAME)
                    .status(errorMessage == null ? "SUCCESS" : "FAILED")
                    .processedCount(processedCount)
                    .errorMessage(errorMessage)
                    .executionTimeMs(Duration.between(startedAt, finishedAt).toMillis())
                    .startedAt(startedAt)
                    .finishedAt(finishedAt)
                    .build());
        } catch (Exception e) {
            log.error("[{}] 스케줄러 로그 저장 실패 - {}", JOB_NAME, e.getMessage(), e);
        }
    }

}
