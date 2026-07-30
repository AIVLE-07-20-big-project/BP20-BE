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

    @Value("${app.notice-storage-path:uploads/notices}")
    private String storagePath;

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
            Path directory = Paths.get(storagePath).toAbsolutePath().normalize().resolve(String.valueOf(noticeId));
            Files.createDirectories(directory);
            String storedName = UUID.randomUUID() + "." + extension;
            Files.copy(file.getInputStream(), directory.resolve(storedName), StandardCopyOption.REPLACE_EXISTING);

            attachmentRepository.findByNoticeId(noticeId).ifPresent(old -> {
                try {
                    Files.deleteIfExists(directory.resolve(old.getStoredName()));
                } catch (IOException ignored) {
                }
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

    private String extensionOf(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase();
    }
}
