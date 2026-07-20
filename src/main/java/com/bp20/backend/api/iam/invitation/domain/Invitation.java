package com.bp20.backend.api.iam.invitation.domain;

import com.bp20.backend.api.user.domain.User;
import com.bp20.backend.api.user.domain.UserRole;
import com.bp20.backend.global.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "invitations",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_invitations_temporary_password_hash",
                columnNames = "temporary_password_hash"
        ),
        indexes = {
                @Index(name = "idx_invitations_email", columnList = "email"),
                @Index(name = "idx_invitations_target_role", columnList = "target_role"),
                @Index(name = "idx_invitations_expires_at", columnList = "expires_at")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Invitation extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "invitation_id")
    private Long id;

    @Column(nullable = false, length = 100)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_role", nullable = false, length = 20)
    private UserRole targetRole;

    @Column(name = "temporary_password_hash", nullable = false, length = 64)
    private String temporaryPasswordHash;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invited_by_user_id", nullable = false)
    private User invitedBy;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    private LocalDateTime acceptedAt;

    private LocalDateTime revokedAt;

    private Invitation(String email, UserRole targetRole, String temporaryPasswordHash,
                       User invitedBy, LocalDateTime expiresAt) {
        validateTargetRole(targetRole);
        this.email = email;
        this.targetRole = targetRole;
        this.temporaryPasswordHash = temporaryPasswordHash;
        this.invitedBy = invitedBy;
        this.expiresAt = expiresAt;
    }

    public static Invitation create(String email, UserRole targetRole, String temporaryPasswordHash,
                                    User invitedBy, LocalDateTime expiresAt) {
        return new Invitation(email, targetRole, temporaryPasswordHash, invitedBy, expiresAt);
    }

    public boolean isUsable(LocalDateTime now) {
        return acceptedAt == null && revokedAt == null && expiresAt.isAfter(now);
    }

    public void accept(LocalDateTime now) {
        this.acceptedAt = now;
    }

    public void revoke(LocalDateTime now) {
        this.revokedAt = now;
    }

    private static void validateTargetRole(UserRole targetRole) {
        if (targetRole != UserRole.ADMIN && targetRole != UserRole.STORE_OWNER) {
            throw new IllegalArgumentException("초대 대상 역할은 ADMIN 또는 STORE_OWNER여야 합니다.");
        }
    }
}
