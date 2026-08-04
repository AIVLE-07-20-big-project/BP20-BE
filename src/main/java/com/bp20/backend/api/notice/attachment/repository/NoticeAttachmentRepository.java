package com.bp20.backend.api.notice.attachment.repository;

import com.bp20.backend.api.notice.attachment.domain.NoticeAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NoticeAttachmentRepository extends JpaRepository<NoticeAttachment, Long> {
    Optional<NoticeAttachment> findByNoticeId(Long noticeId);
}
