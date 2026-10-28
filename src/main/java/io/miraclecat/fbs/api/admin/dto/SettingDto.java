/**
 * packageName    : io.miraclecat.fbs.api.admin.dto
 * fileName       : SettingDto
 * author         : MiracleCat
 * date           : 2026-05-19
 * description    : 시스템 설정 DTO
 */
package io.miraclecat.fbs.api.admin.dto;

import lombok.*;

import java.time.LocalDateTime;

public class SettingDto {

    /**
     * 설정 목록 조회 요청
     */
    @Getter
    @Setter
    public static class SearchReq {
        private String settingKey;
    }

    /**
     * 설정 목록 조회 응답
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchRes {
        private Long id;
        private String settingKey;
        private String settingValue;
        private String description;
        private LocalDateTime updatedAt;
    }

    /**
     * 설정 상세 조회 응답
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DetailRes {
        private Long id;
        private String settingKey;
        private String settingValue;
        private String description;
        private LocalDateTime updatedAt;
    }

    /**
     * 설정 등록 요청
     */
    @Getter
    @Setter
    public static class CreateReq {
        private String settingKey;
        private String settingValue;
        private String description;
    }

    /**
     * 설정 수정 요청
     */
    @Getter
    @Setter
    public static class UpdateReq {
        private String settingValue;
        private String description;
    }

}
