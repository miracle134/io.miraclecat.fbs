/**
 * packageName    : io.miraclecat.fbs.api.code.entity
 * fileName       : Airport
 * author         : MiracleCat
 * date           : 2026-05-19
 * description    : 공항 엔티티
 */
package io.miraclecat.fbs.api.code.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "airports")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Airport {

    @Id
    @Column(name = "code", length = 3)
    private String code;

    @Column(name = "name_ko", nullable = false, length = 50)
    private String nameKo;

    @Column(name = "name_en", nullable = false, length = 100)
    private String nameEn;

    @Column(name = "city", nullable = false, length = 20)
    private String city;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

}
