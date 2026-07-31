package com.bp20.backend.api.notice.attachment.dto.response;

import com.bp20.backend.api.notice.attachment.domain.NoticeAttachment;

public record NoticeAttachmentResponse(
        Long id,
        String originalName,
        String contentType,
        long size,
        String downloadUrl
) {
    public static NoticeAttachmentResponse from(NoticeAttachment attachment) {
        return new NoticeAttachmentResponse(
                attachment.getId(),
                attachment.getOriginalName(),
                attachment.getContentType(),
                attachment.getSize(),
                "/api/notices/" + attachment.getNotice().getId() + "/attachment"
        );
    }
}
