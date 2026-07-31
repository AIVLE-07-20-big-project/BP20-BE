package com.bp20.backend.api.notice.controller;

import com.bp20.backend.api.notice.attachment.service.NoticeAttachmentService;
import com.bp20.backend.api.notice.dto.response.NoticeResponse;
import com.bp20.backend.api.notice.service.NoticeService;
import com.bp20.backend.global.response.ApiResponse;
import com.bp20.backend.global.response.SuccessCode;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notices")
public class NoticeAttachmentController {

    private final NoticeAttachmentService noticeAttachmentService;
    private final NoticeService noticeService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<NoticeResponse>>> getPublishedNotices() {
        return ApiResponse.success(SuccessCode.SUCCESS_NOTICE_GET, noticeService.getPublishedNotices());
    }

    @GetMapping("/{noticeId}/attachment")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable Long noticeId) {
        var attachment = noticeAttachmentService.getAttachment(noticeId);
        Resource resource = noticeAttachmentService.loadResource(attachment);
        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(attachment.getContentType());
        } catch (Exception ignored) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(attachment.getOriginalName()).build().toString())
                .body(resource);
    }
}
