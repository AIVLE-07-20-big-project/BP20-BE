package com.bp20.backend.api.iam.invitation.dto.response;

import com.bp20.backend.api.iam.invitation.domain.Invitation;
import com.bp20.backend.api.iam.invitation.domain.InvitationStatus;
import com.bp20.backend.api.user.domain.UserRole;
import com.bp20.backend.global.util.PersonalDataMasker;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "초대 관리 목록 응답")
public record InvitationSummaryResponse(
        Long id, String email, UserRole targetRole, Long invitedByUserId,
        String invitedByName, InvitationStatus status, LocalDateTime createdAt,
        LocalDateTime expiresAt, LocalDateTime acceptedAt, LocalDateTime revokedAt
) {
    public static InvitationSummaryResponse from(Invitation invitation, LocalDateTime now) {
        return new InvitationSummaryResponse(
                invitation.getId(),
                PersonalDataMasker.email(invitation.getEmail()),
                invitation.getTargetRole(),
                invitation.getInvitedBy().getId(),
                PersonalDataMasker.name(invitation.getInvitedBy().getName()),
                InvitationStatus.from(invitation, now), invitation.getCreatedAt(),
                invitation.getExpiresAt(), invitation.getAcceptedAt(), invitation.getRevokedAt()
        );
    }
}
