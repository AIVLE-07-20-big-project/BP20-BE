package com.bp20.backend.api.notice.service;

import com.bp20.backend.api.notice.domain.Notice;
import com.bp20.backend.api.notice.domain.NoticeStatus;
import com.bp20.backend.api.notice.dto.request.NoticeRequest;
import com.bp20.backend.api.notice.dto.response.NoticeResponse;
import com.bp20.backend.api.notice.repository.NoticeRepository;
import com.bp20.backend.api.notice.attachment.repository.NoticeAttachmentRepository;
import com.bp20.backend.api.notice.attachment.service.NoticeAttachmentService;
import com.bp20.backend.api.user.domain.User;
import com.bp20.backend.api.user.repository.UserRepository;
import com.bp20.backend.global.exception.ApiException;
import com.bp20.backend.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NoticeService {

    private final NoticeRepository noticeRepository;
    private final UserRepository userRepository;
    private final NoticeAttachmentRepository attachmentRepository;
    private final NoticeAttachmentService noticeAttachmentService;

    @Transactional(readOnly = true)
    public List<NoticeResponse> getNotices() {
        return noticeRepository.findAllByOrderByUpdatedAtDesc().stream()
                .map(notice -> NoticeResponse.from(notice, attachmentRepository.findByNoticeId(notice.getId()).orElse(null)))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<NoticeResponse> getPublishedNotices() {
        return noticeRepository.findByStatusOrderByUpdatedAtDesc(NoticeStatus.PUBLISHED).stream()
                .map(notice -> NoticeResponse.from(notice, attachmentRepository.findByNoticeId(notice.getId()).orElse(null)))
                .toList();
    }

    @Transactional
    public NoticeResponse create(Long authorId, NoticeRequest request) {
        User author = getUser(authorId);
        return NoticeResponse.from(noticeRepository.save(Notice.create(
                request.title(), request.body(), request.category(), request.audience(),
                request.status(), request.urgent(), author
        )));
    }

    @Transactional
    public NoticeResponse update(Long noticeId, NoticeRequest request) {
        Notice notice = getNotice(noticeId);
        notice.update(request.title(), request.body(), request.category(), request.audience(),
                request.status(), request.urgent());
        return NoticeResponse.from(notice);
    }

    @Transactional
    public NoticeResponse end(Long noticeId) {
        Notice notice = getNotice(noticeId);
        notice.end();
        return NoticeResponse.from(notice);
    }

    @Transactional
    public void delete(Long noticeId) {
        Notice notice = getNotice(noticeId);
        noticeAttachmentService.deleteByNoticeId(noticeId);
        noticeRepository.delete(notice);
    }

    private Notice getNotice(Long id) {
        return noticeRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND_NOTICE));
    }

    private User getUser(Long id) {
        return userRepository.findByIdWithPrivateInfo(id)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND_USER));
    }
}
