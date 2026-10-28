/**
 * packageName    : io.miraclecat.fbs.api.code.entity
 * fileName       : Airline
 * author         : MiracleCat
 * date           : 2026-05-19
 * description    : 항공사 엔티티
 */
package io.miraclecat.fbs.api.code.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "airlines")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Airline {

    @Id
    @Column(name = "code", length = 2)
    private String code;

    @Column(name = "name_ko", nullable = false, length = 50)
    private String nameKo;

    @Column(name = "name_en", nullable = false, length = 50)
    private String nameEn;

    @Column(name = "logo_url", length = 255)
    private String logoUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

}
