/**
 * packageName    : io.miraclecat.fbs.core.utils
 * fileName       : PageRes
 * author         : MiracleCat
 * date           : 2026-05-19
 * description    : 페이징 응답 공통 DTO
 */
package io.miraclecat.fbs.core.utils.persistence;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class PageRes<T> {

    private List<T> content;    // 데이터 목록
    private long totalCount;    // 전체 건수
    private int page;           // 현재 페이지
    private int size;           // 페이지당 건수
    private int totalPages;     // 전체 페이지 수
    private boolean hasNext;    // 다음 페이지 여부

    public static <T> PageRes<T> of(List<T> content, long totalCount, PageReq pageReq) {
        int totalPages = (int) Math.ceil((double) totalCount / pageReq.getSize());
        return PageRes.<T>builder()
                .content(content)
                .totalCount(totalCount)
                .page(pageReq.getPage())
                .size(pageReq.getSize())
                .totalPages(totalPages)
                .hasNext(pageReq.getPage() < totalPages - 1)
                .build();
    }

}