package com.bp20.backend.api.notice.attachment.service;

import com.bp20.backend.api.notice.attachment.domain.NoticeAttachment;
import com.bp20.backend.api.notice.attachment.repository.NoticeAttachmentRepository;
import com.bp20.backend.api.notice.domain.Notice;
import com.bp20.backend.api.notice.repository.NoticeRepository;
import com.bp20.backend.global.exception.ApiException;
import com.bp20.backend.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.nio.file.*;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NoticeAttachmentService {

    private static final long MAX_SIZE = 20 * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "png", "jpg", "jpeg", "gif", "doc", "docx", "xls", "xlsx");

    private final NoticeRepository noticeRepository;
    private final NoticeAttachmentRepository attachmentRepository;
    private final S3Client s3Client;

    @Value("${app.notice-storage-path:uploads/notices}")
    private String storagePath;

    @Value("${notice.storage.s3.bucket:}")
    private String s3Bucket;

    @Value("${notice.storage.s3.prefix:notices/v1}")
    private String s3Prefix;

    @Transactional
    public NoticeAttachment upload(Long noticeId, MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > MAX_SIZE) {
            throw new IllegalArgumentException("첨부 파일은 20MB 이하이어야 합니다.");
        }
        String originalName = file.getOriginalFilename() == null ? "attachment" : Paths.get(file.getOriginalFilename()).getFileName().toString();
        String extension = extensionOf(originalName);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("지원하지 않는 파일 형식입니다.");
        }
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND_NOTICE));

        try {
            String storedName = buildS3Key(noticeId, extension);
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(requiredS3Bucket())
                    .key(storedName)
                    .contentType(file.getContentType() == null ? "application/octet-stream" : file.getContentType())
                    .build();
            s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            attachmentRepository.findByNoticeId(noticeId).ifPresent(old -> {
                deleteStoredObject(old);
                attachmentRepository.delete(old);
            });
            return attachmentRepository.save(NoticeAttachment.create(
                    notice, originalName, storedName,
                    file.getContentType() == null ? "application/octet-stream" : file.getContentType(),
                    file.getSize()
            ));
        } catch (IOException exception) {
            throw new IllegalStateException("첨부 파일을 저장하지 못했습니다.", exception);
        }
    }

    @Transactional(readOnly = true)
    public NoticeAttachment getAttachment(Long noticeId) {
        return attachmentRepository.findByNoticeId(noticeId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND_NOTICE));
    }

    public Resource loadResource(NoticeAttachment attachment) {
        if (isS3Key(attachment.getStoredName())) {
            try {
                return new org.springframework.core.io.InputStreamResource(s3Client.getObject(GetObjectRequest.builder()
                        .bucket(requiredS3Bucket())
                        .key(attachment.getStoredName())
                        .build()));
            } catch (RuntimeException exception) {
                throw new IllegalStateException("S3 첨부 파일을 읽지 못했습니다.", exception);
            }
        }

        try {
            Path path = Paths.get(storagePath).toAbsolutePath().normalize()
                    .resolve(String.valueOf(attachment.getNotice().getId()))
                    .resolve(attachment.getStoredName()).normalize();
            Resource resource = new UrlResource(path.toUri());
            if (!resource.exists()) throw new IllegalStateException("첨부 파일을 찾을 수 없습니다.");
            return resource;
        } catch (IOException exception) {
            throw new IllegalStateException("첨부 파일을 읽지 못했습니다.", exception);
        }
    }

    @Transactional
    public void deleteByNoticeId(Long noticeId) {
        attachmentRepository.findByNoticeId(noticeId).ifPresent(attachment -> {
            deleteStoredObject(attachment);
            attachmentRepository.delete(attachment);
        });
    }

    private String buildS3Key(Long noticeId, String extension) {
        return normalizePrefix() + "/" + noticeId + "/" + UUID.randomUUID() + "." + extension;
    }

    private boolean isS3Key(String storedName) {
        return storedName != null && storedName.startsWith(normalizePrefix() + "/");
    }

    private void deleteStoredObject(NoticeAttachment attachment) {
        String storedName = attachment.getStoredName();
        if (isS3Key(storedName)) {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(requiredS3Bucket())
                    .key(storedName)
                    .build());
            return;
        }

        try {
            Path path = Paths.get(storagePath).toAbsolutePath().normalize()
                    .resolve(String.valueOf(attachment.getNotice().getId()))
                    .resolve(storedName).normalize();
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // Legacy local files are best-effort cleanup targets.
        }
    }

    private String requiredS3Bucket() {
        if (s3Bucket == null || s3Bucket.isBlank()) {
            throw new IllegalStateException("공지 첨부파일용 S3 버킷이 설정되지 않았습니다. NOTICE_S3_BUCKET 또는 S3_BUCKET_NAME을 설정하세요.");
        }
        return s3Bucket;
    }

    private String normalizePrefix() {
        return s3Prefix.replaceAll("^/+|/+$", "");
    }

    private String extensionOf(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase();
    }
}
