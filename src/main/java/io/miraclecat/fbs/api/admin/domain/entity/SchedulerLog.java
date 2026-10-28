/**
 * packageName    : io.miraclecat.fbs.api.admin.entity
 * fileName       : SchedulerLog
 * author         : MiracleCat
 * date           : 2026-05-19
 * description    : 스케줄러 로그 엔티티
 */
package io.miraclecat.fbs.api.admin.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "scheduler_logs")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SchedulerLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_name", nullable = false, length = 100)
    private String jobName;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "processed_count")
    private Integer processedCount;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "execution_time_ms")
    private Long executionTimeMs;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Builder
    public SchedulerLog(String jobName, String status, Integer processedCount,
                        String errorMessage, Long executionTimeMs,
                        LocalDateTime startedAt, LocalDateTime finishedAt) {
        this.jobName = jobName;
        this.status = status;
        this.processedCount = processedCount;
        this.errorMessage = errorMessage;
        this.executionTimeMs = executionTimeMs;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
    }

}
