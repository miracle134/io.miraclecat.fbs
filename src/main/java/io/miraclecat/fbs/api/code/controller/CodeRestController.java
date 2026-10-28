/**
 * packageName    : io.miraclecat.fbs.api.code.controller
 * fileName       : CodeRestController
 * author         : MiracleCat
 * date           : 2026-05-19
 * description    : 코드 컨트롤러 (항공사, 공항)
 */
package io.miraclecat.fbs.api.code.controller;

import io.miraclecat.fbs.api.code.dto.AirlineDto;
import io.miraclecat.fbs.api.code.dto.AirportDto;
import io.miraclecat.fbs.api.code.service.CodeService;
import io.miraclecat.fbs.core.response.ApiEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class CodeRestController {

    private final CodeService codeService;

    /**
     * 항공사 목록 조회
     * @param req req
     * @return ApiEntity
     */
    @GetMapping("/api/code/airlines")
    public ApiEntity<?> getAirlines(@ModelAttribute AirlineDto.SearchReq req) {
        return codeService.getAirlines(req);
    }

    /**
     * 공항 목록 조회
     * @param req req
     * @return ApiEntity
     */
    @GetMapping("/api/code/airports")
    public ApiEntity<?> getAirports(@ModelAttribute AirportDto.SearchReq req) {
        return codeService.getAirports(req);
    }

}
