/**
 * packageName    : io.miraclecat.fbs.api.code.service
 * fileName       : CodeService
 * author         : MiracleCat
 * date           : 2026-05-19
 * description    : 코드 서비스
 */
package io.miraclecat.fbs.api.code.service;

import io.miraclecat.fbs.api.code.dao.CodeDao;
import io.miraclecat.fbs.api.code.dto.AirlineDto;
import io.miraclecat.fbs.api.code.dto.AirportDto;
import io.miraclecat.fbs.core.config.ReadTx;
import io.miraclecat.fbs.core.response.ApiEntity;
import io.miraclecat.fbs.core.response.HttpsStatusCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CodeService {

    private final CodeDao codeDao;

    /**
     * 항공사 목록 조회
     */
    @ReadTx
    public ApiEntity<List<AirlineDto.SearchRes>> getAirlines(AirlineDto.SearchReq req) {
        List<AirlineDto.SearchRes> airlines = codeDao.findAirlines(req);
        return ApiEntity.of(HttpsStatusCode.SUCCESS, airlines);
    }

    /**
     * 공항 목록 조회
     */
    @ReadTx
    public ApiEntity<List<AirportDto.SearchRes>> getAirports(AirportDto.SearchReq req) {
        List<AirportDto.SearchRes> airports = codeDao.findAirports(req);
        return ApiEntity.of(HttpsStatusCode.SUCCESS, airports);
    }

}
