package com.bp20.backend.api.auth.dto.response;

import com.bp20.backend.api.user.domain.User;
import com.bp20.backend.api.user.domain.UserRole;
import com.bp20.backend.api.user.domain.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "회원가입 응답")
public record SignupResponse(
        @Schema(description = "JWT 액세스 토큰", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
        String accessToken,

        @Schema(description = "토큰 유형", example = "Bearer")
        String tokenType,

        @Schema(description = "사용자 ID", example = "1")
        Long id,

        @Schema(description = "사용자 이메일", example = "user@bp20.com")
        String email,

        @Schema(description = "사용자 이름", example = "김사용자")
        String name,

        @Schema(description = "사용자 연락처", example = "010-1234-5678")
        String phoneNumber,

        @Schema(description = "초대에 따라 결정된 역할", example = "STORE_OWNER")
        UserRole role,

        @Schema(description = "계정 상태", example = "ACTIVE")
        UserStatus status
) {
    public static SignupResponse of(String accessToken, User user) {
        return new SignupResponse(
                accessToken,
                "Bearer",
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getPhoneNumber(),
                user.getRole(),
                user.getStatus()
        );
    }
}
