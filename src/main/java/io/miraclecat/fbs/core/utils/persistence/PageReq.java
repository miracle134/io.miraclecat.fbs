/**
 * packageName    : io.miraclecat.fbs.core.utils
 * fileName       : PageReq
 * author         : MiracleCat
 * date           : 2026-05-19
 * description    : 페이징 요청 공통 DTO
 */
package io.miraclecat.fbs.core.utils.persistence;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@Getter
@Setter
public class PageReq {

    private int page = 0;       // 현재 페이지 (0부터 시작)
    private int size = 10;      // 페이지당 건수

    public Pageable toPageable() {
        return PageRequest.of(page, size);
    }

    public long getOffset() {
        return (long) page * size;
    }

}