package com.bp20.backend.api.iam.invitation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "사용자 초대 생성 요청")
public record InvitationRequest(
        @Schema(description = "초대할 관리자 또는 점주의 이메일", example = "store-owner@bp20.com")
        @Email @NotBlank @Size(max = 100)
        String email,

        @Schema(description = "초대를 실행하는 현재 관리자의 비밀번호", example = "bp20admin001")
        @NotBlank @Size(max = 72)
        String currentPassword
) {
}
