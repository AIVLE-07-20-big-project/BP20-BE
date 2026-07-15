package com.bp20.backend.api.admin.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "점주 초대 생성 요청")
public record CreateStoreOwnerInvitationRequest(
        @Schema(description = "초대할 점주의 이메일", example = "store-owner@bp20.com")
        @Email @NotBlank @Size(max = 100)
        String email,

        @Schema(description = "초대 작업 재인증을 위한 현재 관리자 비밀번호", example = "StrongPassw0rd!23")
        @NotBlank @Size(max = 72)
        String currentPassword
) {
}
