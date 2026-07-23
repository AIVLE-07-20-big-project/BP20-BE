package com.bp20.backend.api.iam.admin.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "관리자 상태 변경 재인증 요청")
public record ChangeAdminStatusRequest(
        @Schema(description = "현재 최상위 관리자 비밀번호", example = "StrongPassw0rd!23")
        @NotBlank @Size(max = 72)
        String currentPassword
) {
}
