package com.bp20.backend.api.notice.repository;

import com.bp20.backend.api.notice.domain.Notice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import com.bp20.backend.api.notice.domain.NoticeStatus;

public interface NoticeRepository extends JpaRepository<Notice, Long> {
    List<Notice> findAllByOrderByUpdatedAtDesc();
    List<Notice> findByStatusOrderByUpdatedAtDesc(NoticeStatus status);
}
