package com.bp20.backend.api.admin.dto.response;

import com.bp20.backend.api.invitation.domain.AdminInvitation;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "관리자 초대 생성 응답")
public record AdminInvitationResponse(
        @Schema(description = "초대 ID", example = "1")
        Long id,
        @Schema(description = "초대 대상 이메일", example = "admin@bp20.com")
        String email,
        @Schema(description = "초대 만료 일시")
        LocalDateTime expiresAt,
        @Schema(description = "한 번만 반환되는 관리자 임시 비밀번호")
        String temporaryPassword
) {
    public static AdminInvitationResponse from(AdminInvitation invitation, String temporaryPassword) {
        return new AdminInvitationResponse(
                invitation.getId(), invitation.getEmail(), invitation.getExpiresAt(), temporaryPassword
        );
    }
}
