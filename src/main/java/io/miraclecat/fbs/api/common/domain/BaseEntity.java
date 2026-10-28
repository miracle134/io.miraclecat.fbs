package io.miraclecat.fbs.api.common.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.hibernate.annotations.Comment;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * packageName    : io.miraclecat.fbs.domain
 * fileName       : InsertUpdateLog
 * author         : MiracleCat
 * date           : 2024-05-02
 * description    :
 */
@Getter
@EntityListeners(value = AuditingEntityListener.class)
@MappedSuperclass
public abstract class BaseEntity {

    @Comment("등록자")
    @CreatedBy
    @Column(name = "created_id", updatable = false)
    private Long createdId;
    
    @Comment("수정자")
    @LastModifiedBy
    @Column(name = "updated_id", insertable = false)
    private Long updatedId;

    @Comment("등록일")
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Comment("수정일")
    @LastModifiedDate
    @Column(name = "updated_at", insertable = false)
    private LocalDateTime updatedAt;
    
}
