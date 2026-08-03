package com.bp20.backend.api.iam.log.domain;

import com.bp20.backend.api.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_user_id")
    private User actorUser;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private IamLogAction action;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_user_id")
    private User targetUser;

    @Column(length = 100)
    private String targetEmail;

    @Column(nullable = false, length = 45)
    private String sourceIp;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private IamLog(User actorUser, IamLogAction action, User targetUser,
                   String targetEmail, String sourceIp) {
        this.actorUser = actorUser;
        this.action = action;
        this.targetUser = targetUser;
        this.targetEmail = targetEmail;
        this.sourceIp = sourceIp;
    }

    public static IamLog of(User actorUser, IamLogAction action, User targetUser,
                            String targetEmail, String sourceIp) {
        return new IamLog(actorUser, action, targetUser, targetEmail, sourceIp);
    }

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
