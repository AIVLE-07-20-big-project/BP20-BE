package com.bp20.backend.api.notice.dto.response;

import com.bp20.backend.api.notice.domain.Notice;
import com.bp20.backend.api.notice.domain.NoticeStatus;
import com.bp20.backend.api.notice.attachment.domain.NoticeAttachment;
import com.bp20.backend.api.notice.attachment.dto.response.NoticeAttachmentResponse;

import java.time.LocalDateTime;

public record NoticeResponse(
        Long id,
        String title,
        String body,
        String category,
        String audience,
        NoticeStatus status,
        boolean urgent,
        String author,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        NoticeAttachmentResponse attachment
) {
    public static NoticeResponse from(Notice notice) {
        return from(notice, null);
    }

    public static NoticeResponse from(Notice notice, NoticeAttachment attachment) {
        return new NoticeResponse(
                notice.getId(), notice.getTitle(), notice.getBody(), notice.getCategory(),
                notice.getAudience(), notice.getStatus(), notice.isUrgent(),
                notice.getAuthor().getName(), notice.getCreatedAt(), notice.getUpdatedAt(),
                attachment == null ? null : NoticeAttachmentResponse.from(attachment)
        );
    }
}
