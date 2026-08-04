package com.bp20.backend.api.notice.domain;

import com.bp20.backend.api.user.domain.User;
import com.bp20.backend.global.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "notices", indexes = {
        @Index(name = "idx_notices_status", columnList = "status"),
        @Index(name = "idx_notices_updated_at", columnList = "updatedAt")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notice extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Lob
    @Column(nullable = false)
    private String body;

    @Column(nullable = false, length = 30)
    private String category;

    @Column(nullable = false, length = 30)
    private String audience;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NoticeStatus status;

    @Column(nullable = false)
    private boolean urgent;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_notices_author"))
    private User author;

    private Notice(String title, String body, String category, String audience,
                   NoticeStatus status, boolean urgent, User author) {
        this.title = title;
        this.body = body;
        this.category = category;
        this.audience = audience;
        this.status = status;
        this.urgent = urgent;
        this.author = author;
    }

    public static Notice create(String title, String body, String category, String audience,
                                NoticeStatus status, boolean urgent, User author) {
        return new Notice(title, body, category, audience, status, urgent, author);
    }

    public void update(String title, String body, String category, String audience,
                       NoticeStatus status, boolean urgent) {
        this.title = title;
        this.body = body;
        this.category = category;
        this.audience = audience;
        this.status = status;
        this.urgent = urgent;
    }

    public void end() {
        this.status = NoticeStatus.ENDED;
    }
}
