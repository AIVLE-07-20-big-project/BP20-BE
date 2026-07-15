package com.bp20.backend.api.iam.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "iam_logs", indexes = {
        @Index(name = "idx_iam_logs_actor", columnList = "actor_user_id"),
        @Index(name = "idx_iam_logs_action", columnList = "action"),
        @Index(name = "idx_iam_logs_created_at", columnList = "created_at")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IamLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "iam_log_id")
    private Long id;

    private Long actorUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private IamLogAction action;

    private Long targetUserId;

    @Column(length = 100)
    private String targetEmail;

    @Column(nullable = false, length = 45)
    private String sourceIp;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private IamLog(Long actorUserId, IamLogAction action, Long targetUserId,
                   String targetEmail, String sourceIp) {
        this.actorUserId = actorUserId;
        this.action = action;
        this.targetUserId = targetUserId;
        this.targetEmail = targetEmail;
        this.sourceIp = sourceIp;
    }

    public static IamLog of(Long actorUserId, IamLogAction action, Long targetUserId,
                            String targetEmail, String sourceIp) {
        return new IamLog(actorUserId, action, targetUserId, targetEmail, sourceIp);
    }

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
