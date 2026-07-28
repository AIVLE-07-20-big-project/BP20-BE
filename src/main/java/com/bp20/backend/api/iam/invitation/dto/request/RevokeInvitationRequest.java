package com.bp20.backend.api.iam.invitation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "초대 취소 재인증 요청")
public record RevokeInvitationRequest(
        @Schema(description = "현재 관리자 비밀번호", example = "StrongPassw0rd!23")
        @NotBlank @Size(max = 72)
        String currentPassword
) {
}
