/**
 * packageName    : io.miraclecat.fbs.api.admin.dao
 * fileName       : AdminDao
 * author         : MiracleCat
 * date           : 2026-05-19
 * description    : Admin 도메인 QueryDSL 조회
 */
package io.miraclecat.fbs.api.admin.dao;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import io.miraclecat.fbs.api.admin.domain.entity.QSchedulerLog;
import io.miraclecat.fbs.api.admin.domain.entity.QSetting;
import io.miraclecat.fbs.api.admin.dto.SchedulerLogDto;
import io.miraclecat.fbs.api.admin.dto.SettingDto;
import io.miraclecat.fbs.core.utils.persistence.QueryDslHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class AdminDao {

    private final JPAQueryFactory queryFactory;

    private final QSetting setting = QSetting.setting;
    private final QSchedulerLog schedulerLog = QSchedulerLog.schedulerLog;

    /**
     * 설정 목록 조회
     */
    public List<SettingDto.SearchRes> findSettings(SettingDto.SearchReq req) {
        return queryFactory
                .select(Projections.bean(SettingDto.SearchRes.class,
                        setting.id,
                        setting.settingKey,
                        setting.settingValue,
                        setting.description,
                        setting.updatedAt
                ))
                .from(setting)
                .where(QueryDslHelper.contains(setting.settingKey, req.getSettingKey()))
                .orderBy(setting.settingKey.asc())
                .fetch();
    }

    /**
     * 설정 상세 조회
     */
    public SettingDto.DetailRes findSettingDetail(Long settingId) {
        return queryFactory
                .select(Projections.bean(SettingDto.DetailRes.class,
                        setting.id,
                        setting.settingKey,
                        setting.settingValue,
                        setting.description,
                        setting.updatedAt
                ))
                .from(setting)
                .where(setting.id.eq(settingId))
                .fetchOne();
    }

    /**
     * 스케줄러 로그 목록 조회
     */
    public List<SchedulerLogDto.SearchRes> findSchedulerLogs(SchedulerLogDto.SearchReq req) {
        return queryFactory
                .select(Projections.bean(SchedulerLogDto.SearchRes.class,
                        schedulerLog.id,
                        schedulerLog.jobName,
                        schedulerLog.status,
                        schedulerLog.processedCount,
                        schedulerLog.executionTimeMs,
                        schedulerLog.startedAt,
                        schedulerLog.finishedAt
                ))
                .from(schedulerLog)
                .where(
                        QueryDslHelper.contains(schedulerLog.jobName, req.getJobName()),
                        QueryDslHelper.eq(schedulerLog.status, req.getStatus()),
                        QueryDslHelper.goe(schedulerLog.startedAt, req.getStartedAtFrom()),
                        QueryDslHelper.loe(schedulerLog.startedAt, req.getStartedAtTo())
                )
                .orderBy(schedulerLog.startedAt.desc())
                .fetch();
    }

    /**
     * 스케줄러 로그 상세 조회
     */
    public SchedulerLogDto.DetailRes findSchedulerLogDetail(Long schedulerLogId) {
        return queryFactory
                .select(Projections.bean(SchedulerLogDto.DetailRes.class,
                        schedulerLog.id,
                        schedulerLog.jobName,
                        schedulerLog.status,
                        schedulerLog.processedCount,
                        schedulerLog.errorMessage,
                        schedulerLog.executionTimeMs,
                        schedulerLog.startedAt,
                        schedulerLog.finishedAt
                ))
                .from(schedulerLog)
                .where(schedulerLog.id.eq(schedulerLogId))
                .fetchOne();
    }

    // ==================== 조건 ====================

//    private BooleanExpression settingKeyContains(String settingKey) {
//        return StringUtils.hasText(settingKey) ? setting.settingKey.contains(settingKey) : null;
//    }
//
//    private BooleanExpression jobNameContains(String jobName) {
//        return StringUtils.hasText(jobName) ? schedulerLog.jobName.contains(jobName) : null;
//    }
//
//    private BooleanExpression statusEq(String status) {
//        return StringUtils.hasText(status) ? schedulerLog.status.eq(status) : null;
//    }
//
//    private BooleanExpression startedAtGoe(LocalDateTime from) {
//        return from != null ? schedulerLog.startedAt.goe(from) : null;
//    }
//
//    private BooleanExpression startedAtLoe(LocalDateTime to) {
//        return to != null ? schedulerLog.startedAt.loe(to) : null;
//    }

}
