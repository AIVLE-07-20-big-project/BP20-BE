package com.bp20.backend.api.invitation.domain;

import com.bp20.backend.api.user.domain.User;
import com.bp20.backend.global.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
        name = "admin_invitations",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_admin_invitations_temporary_password_hash",
                columnNames = "temporary_password_hash"
        ),
        indexes = {
                @Index(name = "idx_admin_invitations_email", columnList = "email"),
                @Index(name = "idx_admin_invitations_expires_at", columnList = "expires_at")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminInvitation extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "admin_invitation_id")
    private Long id;

    @Column(nullable = false, length = 100)
    private String email;

    @Column(name = "temporary_password_hash", nullable = false, length = 64)
    private String temporaryPasswordHash;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invited_by_user_id", nullable = false)
    private User invitedBy;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    private LocalDateTime acceptedAt;

    private LocalDateTime revokedAt;

    private AdminInvitation(String email, String temporaryPasswordHash, User invitedBy, LocalDateTime expiresAt) {
        this.email = email;
        this.temporaryPasswordHash = temporaryPasswordHash;
        this.invitedBy = invitedBy;
        this.expiresAt = expiresAt;
    }

    public static AdminInvitation create(String email, String temporaryPasswordHash,
                                         User invitedBy, LocalDateTime expiresAt) {
        return new AdminInvitation(email, temporaryPasswordHash, invitedBy, expiresAt);
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
}
