package com.bp20.backend.api.notice.controller;

import com.bp20.backend.api.notice.dto.request.NoticeRequest;
import com.bp20.backend.api.notice.dto.response.NoticeResponse;
import com.bp20.backend.api.notice.attachment.dto.response.NoticeAttachmentResponse;
import com.bp20.backend.api.notice.attachment.service.NoticeAttachmentService;
import com.bp20.backend.api.notice.service.NoticeService;
import com.bp20.backend.global.response.ApiResponse;
import com.bp20.backend.global.response.SuccessCode;
import com.bp20.backend.global.security.principal.SecurityPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/notices")
@Tag(name = "관리자 - 공지", description = "관리자 공지 게시판 CRUD API")
@SecurityRequirement(name = "bearerAuth")
public class NoticeController {

    private final NoticeService noticeService;
    private final NoticeAttachmentService noticeAttachmentService;

    @GetMapping
    @Operation(summary = "공지 목록 조회")
    public ResponseEntity<ApiResponse<List<NoticeResponse>>> getNotices() {
        return ApiResponse.success(SuccessCode.SUCCESS_NOTICE_GET, noticeService.getNotices());
    }

    @PostMapping
    @Operation(summary = "공지 작성")
    public ResponseEntity<ApiResponse<NoticeResponse>> create(
            @AuthenticationPrincipal SecurityPrincipal currentUser,
            @Valid @RequestBody NoticeRequest request
    ) {
        return ApiResponse.success(SuccessCode.SUCCESS_NOTICE_CREATE,
                noticeService.create(currentUser.id(), request));
    }

    @PutMapping("/{noticeId}")
    @Operation(summary = "공지 수정")
    public ResponseEntity<ApiResponse<NoticeResponse>> update(
            @PathVariable Long noticeId,
            @Valid @RequestBody NoticeRequest request
    ) {
        return ApiResponse.success(SuccessCode.SUCCESS_NOTICE_UPDATE,
                noticeService.update(noticeId, request));
    }

    @PatchMapping("/{noticeId}/end")
    @Operation(summary = "공지 게시 종료")
    public ResponseEntity<ApiResponse<NoticeResponse>> end(@PathVariable Long noticeId) {
        return ApiResponse.success(SuccessCode.SUCCESS_NOTICE_END, noticeService.end(noticeId));
    }

    @DeleteMapping("/{noticeId}")
    @Operation(summary = "공지 삭제")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long noticeId) {
        noticeService.delete(noticeId);
        return ApiResponse.successOnly(SuccessCode.SUCCESS_NOTICE_DELETE);
    }

    @PostMapping(value = "/{noticeId}/attachment", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "공지 첨부 파일 업로드")
    public ResponseEntity<ApiResponse<NoticeAttachmentResponse>> uploadAttachment(
            @PathVariable Long noticeId,
            @RequestPart("file") MultipartFile file
    ) {
        return ApiResponse.success(SuccessCode.SUCCESS_NOTICE_ATTACHMENT_UPLOAD,
                NoticeAttachmentResponse.from(noticeAttachmentService.upload(noticeId, file)));
    }

    @GetMapping("/{noticeId}/attachment")
    @Operation(summary = "공지 첨부 파일 다운로드")
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
                        ContentDisposition.attachment()
                                .filename(attachment.getOriginalName())
                                .build().toString())
                .body(resource);
    }
}
