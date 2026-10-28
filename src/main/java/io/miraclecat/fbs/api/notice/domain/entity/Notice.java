/**
 * packageName    : io.miraclecat.fbs.api.notice.entity
 * fileName       : Notice
 * author         : MiracleCat
 * date           : 2026-05-19
 * description    : 공지사항 엔티티
 */
package io.miraclecat.fbs.api.notice.domain.entity;

import io.miraclecat.fbs.api.notice.dto.NoticeDto;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "notices")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notice {

    public static final String STATUS_PUBLISHED = "published";
    public static final String STATUS_HIDDEN = "hidden";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "is_important", nullable = false)
    private Boolean isImportant;

    @Column(name = "views", nullable = false)
    private Integer views;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "created_id")
    private Long createdId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_id")
    private Long updatedId;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public Notice(String title, String content, Boolean isImportant, String status, Long createdId) {
        this.title = title;
        this.content = content;
        this.isImportant = isImportant != null ? isImportant : false;
        this.views = 0;
        this.status = status != null ? status : STATUS_PUBLISHED;
        this.createdId = createdId;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void updateNotice(NoticeDto.UpdateReq req, Long updatedId) {
        this.title = req.getTitle();
        this.content = req.getContent();
        this.isImportant = req.getIsImportant();
        this.status = req.getStatus();
        this.updatedId = updatedId;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isPublished() {
        return STATUS_PUBLISHED.equals(this.status);
    }

    public void increaseViews() {
        this.views++;
    }

    public void deleteNotice(Long updatedId) {
        this.status = STATUS_HIDDEN;
        this.updatedId = updatedId;
        this.updatedAt = LocalDateTime.now();
    }

}
