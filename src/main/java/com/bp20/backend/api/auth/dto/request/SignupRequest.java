package com.bp20.backend.api.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "초대 기반 회원가입 요청")
public record SignupRequest(
        @Schema(description = "초대받은 이메일", example = "admin@bp20.com")
        @Email @NotBlank @Size(max = 100)
        String email,

        @Schema(description = "초대 시 한 번만 발급되는 일회용 임시 비밀번호", example = "A7!kP3@mN8#qR5$t")
        @NotBlank @Size(max = 64)
        String temporaryPassword,

        @Schema(description = "회원가입 후 사용할 새 비밀번호", example = "bp20admin001")
        @NotBlank @Size(min = 12, max = 72)
        String password,

        @Schema(description = "사용자 이름", example = "BP20 관리자")
        @NotBlank @Size(max = 50)
        String name,

        @Schema(description = "사용자 연락처", example = "010-1234-5678")
        @Size(max = 30)
        String phoneNumber
) {
}
