/**
 * packageName    : io.miraclecat.fbs.api.admin.repository
 * fileName       : SettingRepository
 * author         : MiracleCat
 * date           : 2026-05-19
 * description    : 시스템 설정 Repository
 */
package io.miraclecat.fbs.api.admin.domain.repository;

import io.miraclecat.fbs.api.admin.domain.entity.Setting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SettingRepository extends JpaRepository<Setting, Long> {

    Optional<Setting> findBySettingKey(String settingKey);

}
