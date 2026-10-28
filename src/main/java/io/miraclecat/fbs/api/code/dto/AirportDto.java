/**
 * packageName    : io.miraclecat.fbs.api.code.dto
 * fileName       : AirportDto
 * author         : MiracleCat
 * date           : 2026-05-19
 * description    : 공항 DTO
 */
package io.miraclecat.fbs.api.code.dto;

import lombok.*;

public class AirportDto {

    /**
     * 공항 조회 요청
     */
    @Getter
    @Setter
    public static class SearchReq {
        private String code;
        private String nameKo;
        private String nameEn;
        private String city;
    }

    /**
     * 공항 조회 응답
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchRes {
        private String code;
        private String nameKo;
        private String nameEn;
        private String city;
    }

}
