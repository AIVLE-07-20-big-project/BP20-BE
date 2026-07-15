package com.bp20.backend.api.auth.dto.response;

import com.bp20.backend.api.user.domain.User;
import com.bp20.backend.api.user.domain.UserRole;
import com.bp20.backend.api.user.domain.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "현재 사용자 정보 응답")
public record MeResponse(
        @Schema(description = "사용자 ID", example = "1")
        Long id,

        @Schema(description = "사용자 이메일", example = "store-owner@bp20.com")
        String email,

        @Schema(description = "이름", example = "홍길동")
        String name,

        @Schema(description = "연락처", example = "010-1234-5678")
        String phoneNumber,

        @Schema(description = "역할. 관리자와 점주를 구분합니다.", example = "STORE_OWNER")
        UserRole role,

        @Schema(description = "상태", example = "ACTIVE")
        UserStatus status
) {
    public static MeResponse from(User user) {
        return new MeResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getPhoneNumber(),
                user.getRole(),
                user.getStatus()
        );
    }
}
