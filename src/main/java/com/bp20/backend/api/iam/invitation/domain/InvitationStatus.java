package com.bp20.backend.api.iam.invitation.domain;

import java.time.LocalDateTime;

public enum InvitationStatus {
    PENDING,
    ACCEPTED,
    EXPIRED,
    REVOKED;

    public static InvitationStatus from(Invitation invitation, LocalDateTime now) {
        if (invitation.getAcceptedAt() != null) {
            return ACCEPTED;
        }
        if (invitation.getRevokedAt() != null) {
            return REVOKED;
        }
        if (!invitation.getExpiresAt().isAfter(now)) {
            return EXPIRED;
        }
        return PENDING;
    }
}
