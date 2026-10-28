/**
 * packageName    : io.miraclecat.fbs.api.notice.dto
 * fileName       : NoticeDto
 * author         : MiracleCat
 * date           : 2026-05-19
 * description    : 공지사항 DTO
 */
package io.miraclecat.fbs.api.notice.dto;

import io.miraclecat.fbs.core.utils.persistence.PageReq;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class NoticeDto {

    /**
     * 공지사항 목록 조회 요청 (사용자는 status가 서버에서 published로 고정)
     */
    @Getter
    @Setter
    public static class SearchReq extends PageReq {
        private String title;
        private String authorName;
        private Boolean isImportant;
        private String status;
        @DateTimeFormat(pattern = "yyyy-MM-dd")
        private LocalDate startDate;
        @DateTimeFormat(pattern = "yyyy-MM-dd")
        private LocalDate endDate;
    }

    /**
     * 공지사항 목록 조회 응답
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchRes {
        private Long id;
        private String title;
        private Boolean isImportant;
        private Integer views;
        private String status;
        private String authorName;
        private LocalDateTime createdAt;
    }

    /**
     * 공지사항 상세 조회 응답
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DetailRes {
        private Long id;
        private String title;
        private String content;
        private Boolean isImportant;
        private Integer views;
        private String status;
        private String authorName;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    /**
     * 공지사항 등록 요청
     */
    @Getter
    @Setter
    public static class CreateReq {
        private String title;
        private String content;
        private Boolean isImportant;
        private String status;
    }

    /**
     * 공지사항 수정 요청
     */
    @Getter
    @Setter
    public static class UpdateReq {
        private String title;
        private String content;
        private Boolean isImportant;
        private String status;
    }

}
