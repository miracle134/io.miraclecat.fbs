/**
 * packageName    : io.miraclecat.fbs.api.code.dto
 * fileName       : AirlineDto
 * author         : MiracleCat
 * date           : 2026-05-19
 * description    : 항공사 DTO
 */
package io.miraclecat.fbs.api.code.dto;

import lombok.*;

public class AirlineDto {

    /**
     * 항공사 조회 요청
     */
    @Getter
    @Setter
    public static class SearchReq {
        private String code;
        private String nameKo;
        private String nameEn;
    }

    /**
     * 항공사 조회 응답
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
        private String logoUrl;
    }

}
