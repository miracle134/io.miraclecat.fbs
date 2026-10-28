/**
 * packageName    : io.miraclecat.fbs.api.admin.service
 * fileName       : SettingQueryService
 * author         : MiracleCat
 * date           : 2026-10-25
 * description    : 시스템 설정값 조회 (다른 도메인에서 사용)
 */
package io.miraclecat.fbs.api.admin.service;

import io.miraclecat.fbs.api.admin.domain.entity.Setting;
import io.miraclecat.fbs.api.admin.domain.repository.SettingRepository;
import io.miraclecat.fbs.core.config.ReadTx;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SettingQueryService {

    public static final String BOOKING_FEE_PER_PERSON = "booking_fee_per_person";

    private final SettingRepository settingRepository;

    /**
     * 정수 설정값 조회 (없거나 숫자가 아니면 기본값)
     */
    @ReadTx
    public int getInt(String settingKey, int defaultValue) {
        return settingRepository.findBySettingKey(settingKey)
                .map(Setting::getSettingValue)
                .map(value -> {
                    try {
                        return Integer.parseInt(value.trim());
                    } catch (NumberFormatException e) {
                        log.warn("설정값이 숫자가 아닙니다. key={}, value={}", settingKey, value);
                        return defaultValue;
                    }
                })
                .orElse(defaultValue);
    }

}
