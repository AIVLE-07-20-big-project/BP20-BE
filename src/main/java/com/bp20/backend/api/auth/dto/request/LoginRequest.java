package com.bp20.backend.api.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "로그인 요청")
public record LoginRequest(
        @Schema(description = "관리자 또는 점주의 로그인 이메일", example = "super-admin@bp20.com")
        @Email
        @NotBlank
        @Size(max = 100)
        String email,

        @Schema(description = "비밀번호", example = "bp20superadmin")
        @NotBlank
        @Size(max = 72)
        String password,

        @Schema(description = "브라우저 종료 후에도 로그인 상태를 유지할지 여부", example = "false")
        boolean rememberMe
) {
    public LoginRequest(String email, String password) {
        this(email, password, false);
    }
}
