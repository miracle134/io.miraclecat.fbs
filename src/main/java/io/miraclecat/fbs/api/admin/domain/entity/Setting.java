/**
 * packageName    : io.miraclecat.fbs.api.admin.entity
 * fileName       : Setting
 * author         : MiracleCat
 * date           : 2026-05-19
 * description    : 시스템 설정 엔티티
 */
package io.miraclecat.fbs.api.admin.domain.entity;

import io.miraclecat.fbs.api.admin.dto.SettingDto;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "settings")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Setting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "setting_key", nullable = false, unique = true, length = 50)
    private String settingKey;

    @Column(name = "setting_value", nullable = false, length = 255)
    private String settingValue;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "updated_id")
    private Long updatedId;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public Setting(String settingKey, String settingValue, String description) {
        this.settingKey = settingKey;
        this.settingValue = settingValue;
        this.description = description;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateSetting(SettingDto.UpdateReq req, Long updatedId) {
        this.settingValue = req.getSettingValue();
        this.description = req.getDescription();
        this.updatedId = updatedId;
        this.updatedAt = LocalDateTime.now();
    }

}
