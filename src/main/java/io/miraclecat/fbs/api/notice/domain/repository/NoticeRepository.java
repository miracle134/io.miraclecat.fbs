/**
 * packageName    : io.miraclecat.fbs.api.notice.repository
 * fileName       : NoticeRepository
 * author         : MiracleCat
 * date           : 2026-05-19
 * description    : 공지사항 Repository
 */
package io.miraclecat.fbs.api.notice.domain.repository;

import io.miraclecat.fbs.api.notice.domain.entity.Notice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoticeRepository extends JpaRepository<Notice, Long> {
}
