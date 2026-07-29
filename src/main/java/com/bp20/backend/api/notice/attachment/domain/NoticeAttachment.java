package com.bp20.backend.api.notice.attachment.domain;

import com.bp20.backend.api.notice.domain.Notice;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "notice_attachments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NoticeAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "notice_id", nullable = false, unique = true)
    private Notice notice;

    @Column(nullable = false, length = 255)
    private String originalName;

    @Column(nullable = false, length = 255)
    private String storedName;

    @Column(nullable = false, length = 100)
    private String contentType;

    @Column(nullable = false)
    private long size;

    private NoticeAttachment(Notice notice, String originalName, String storedName,
                             String contentType, long size) {
        this.notice = notice;
        this.originalName = originalName;
        this.storedName = storedName;
        this.contentType = contentType;
        this.size = size;
    }

    public static NoticeAttachment create(Notice notice, String originalName, String storedName,
                                          String contentType, long size) {
        return new NoticeAttachment(notice, originalName, storedName, contentType, size);
    }
}
