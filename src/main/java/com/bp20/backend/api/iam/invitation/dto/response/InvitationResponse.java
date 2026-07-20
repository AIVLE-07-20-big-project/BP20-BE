package com.bp20.backend.api.iam.invitation.dto.response;

import com.bp20.backend.api.iam.invitation.domain.Invitation;
import com.bp20.backend.api.user.domain.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "사용자 초대 생성 응답")
public record InvitationResponse(
        @Schema(description = "초대 ID", example = "1")
        Long id,
        @Schema(description = "초대 대상 이메일", example = "user@bp20.com")
        String email,
        @Schema(description = "초대 대상 역할", example = "STORE_OWNER")
        UserRole targetRole,
        @Schema(description = "초대 만료 일시")
        LocalDateTime expiresAt,
        @Schema(description = "한 번만 반환되는 일회용 임시 비밀번호")
        String temporaryPassword
) {
    public static InvitationResponse from(Invitation invitation, String temporaryPassword) {
        return new InvitationResponse(
                invitation.getId(),
                invitation.getEmail(),
                invitation.getTargetRole(),
                invitation.getExpiresAt(),
                temporaryPassword
        );
    }
}
