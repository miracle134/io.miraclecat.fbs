/**
 * packageName    : io.miraclecat.fbs.api.admin.dto
 * fileName       : SchedulerLogDto
 * author         : MiracleCat
 * date           : 2026-05-19
 * description    : 스케줄러 로그 DTO
 */
package io.miraclecat.fbs.api.admin.dto;

import lombok.*;

import java.time.LocalDateTime;

public class SchedulerLogDto {

    /**
     * 스케줄러 로그 목록 조회 요청
     */
    @Getter
    @Setter
    public static class SearchReq {
        private String jobName;
        private String status;
        private LocalDateTime startedAtFrom;
        private LocalDateTime startedAtTo;
    }

    /**
     * 스케줄러 로그 목록 조회 응답
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchRes {
        private Long id;
        private String jobName;
        private String status;
        private Integer processedCount;
        private Long executionTimeMs;
        private LocalDateTime startedAt;
        private LocalDateTime finishedAt;
    }

    /**
     * 스케줄러 로그 상세 조회 응답
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DetailRes {
        private Long id;
        private String jobName;
        private String status;
        private Integer processedCount;
        private String errorMessage;
        private Long executionTimeMs;
        private LocalDateTime startedAt;
        private LocalDateTime finishedAt;
    }

}
