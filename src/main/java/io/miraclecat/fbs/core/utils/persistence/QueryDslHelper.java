package io.miraclecat.fbs.core.utils.persistence;

import com.querydsl.core.types.dsl.*;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * packageName    : io.miraclecat.fbs.core.utils
 * fileName       : QueryDslHelper
 * author         : MiracleCat
 * date           : 2026-05-19
 * description    : QueryDSL 공통 조건 헬퍼
 *                  - 제네릭: eq, goe, loe (String/Number/Date/DateTime 공통)
 *                  - String 전용: contains
 *                  - Boolean 전용: isTrue, isFalse
 */
public class QueryDslHelper {

    private QueryDslHelper() {}

    // ==================== String 전용 ====================

    /**
     * String LIKE 검색
     */
    public static BooleanExpression contains(StringPath path, String value) {
        return StringUtils.hasText(value) ? path.contains(value) : null;
    }

    /**
     * String 동등 비교
     */
    public static BooleanExpression eq(StringPath path, String value) {
        return StringUtils.hasText(value) ? path.eq(value) : null;
    }

    /** String IN (null/빈 리스트면 조건 제외) */
    public static BooleanExpression in(StringPath path, List<String> values) {
        return values != null && !values.isEmpty() ? path.in(values) : null;
    }

    // ==================== Number 전용 ====================

    /**
     * Long 동등 비교
     */
    public static BooleanExpression eq(NumberPath<Long> path, Long value) {
        return value != null ? path.eq(value) : null;
    }

    /**
     * Integer 동등 비교
     */
    public static BooleanExpression eq(NumberPath<Integer> path, Integer value) {
        return value != null ? path.eq(value) : null;
    }

    // ==================== Date 전용 ====================

    /**
     * LocalDate 이상 (>=)
     */
    public static BooleanExpression goe(DatePath<LocalDate> path, LocalDate value) {
        return value != null ? path.goe(value) : null;
    }

    /**
     * LocalDate 이하 (<=)
     */
    public static BooleanExpression loe(DatePath<LocalDate> path, LocalDate value) {
        return value != null ? path.loe(value) : null;
    }

    // ==================== DateTime 전용 ====================

    /**
     * LocalDateTime 이상 (>=)
     */
    public static BooleanExpression goe(DateTimePath<LocalDateTime> path, LocalDateTime value) {
        return value != null ? path.goe(value) : null;
    }

    /**
     * LocalDateTime 이하 (<=)
     */
    public static BooleanExpression loe(DateTimePath<LocalDateTime> path, LocalDateTime value) {
        return value != null ? path.loe(value) : null;
    }

    /** Number 이상 (>=) */
    public static <T extends Number & Comparable<?>> BooleanExpression goe(NumberPath<T> path, T value) {
        return value != null ? path.goe(value) : null;
    }

    /** Number 이하 (<=) */
    public static <T extends Number & Comparable<?>> BooleanExpression loe(NumberPath<T> path, T value) {
        return value != null ? path.loe(value) : null;
    }

    // ==================== Boolean 전용 ====================

    /**
     * Boolean 동등 비교
     */
    public static BooleanExpression eq(BooleanPath path, Boolean value) {
        return value != null ? path.eq(value) : null;
    }

}