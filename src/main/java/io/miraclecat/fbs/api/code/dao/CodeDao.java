/**
 * packageName    : io.miraclecat.fbs.api.code.dao
 * fileName       : CodeDao
 * author         : MiracleCat
 * date           : 2026-05-19
 * description    : 코드 도메인 QueryDSL 조회
 */
package io.miraclecat.fbs.api.code.dao;

import io.miraclecat.fbs.api.code.domain.entity.QAirline;
import io.miraclecat.fbs.api.code.domain.entity.QAirport;
import io.miraclecat.fbs.api.code.dto.AirlineDto;
import io.miraclecat.fbs.api.code.dto.AirportDto;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import io.miraclecat.fbs.core.utils.persistence.QueryDslHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class CodeDao {

    private final JPAQueryFactory queryFactory;

    private final QAirline airline = QAirline.airline;
    private final QAirport airport = QAirport.airport;

    /**
     * 항공사 목록 조회
     */
    public List<AirlineDto.SearchRes> findAirlines(AirlineDto.SearchReq req) {
        return queryFactory
                .select(Projections.constructor(AirlineDto.SearchRes.class,
                        airline.code,
                        airline.nameKo,
                        airline.nameEn,
                        airline.logoUrl
                ))
                .from(airline)
                .where(
                        QueryDslHelper.eq(airline.code, req.getCode()),
                        QueryDslHelper.contains(airline.nameKo, req.getNameKo()),
                        QueryDslHelper.contains(airline.nameEn, req.getNameEn())
                )
                .orderBy(airline.code.asc())
                .fetch();
    }

    /**
     * 공항 목록 조회
     */
    public List<AirportDto.SearchRes> findAirports(AirportDto.SearchReq req) {
        return queryFactory
                .select(Projections.constructor(AirportDto.SearchRes.class,
                        airport.code,
                        airport.nameKo,
                        airport.nameEn,
                        airport.city
                ))
                .from(airport)
                .where(
                        QueryDslHelper.eq(airport.code, req.getCode()),
                        QueryDslHelper.contains(airport.nameKo, req.getNameKo()),
                        QueryDslHelper.contains(airport.nameEn, req.getNameEn()),
                        QueryDslHelper.contains(airport.city, req.getCity())
                )
                .orderBy(airport.code.asc())
                .fetch();
    }

}
