package com.bp20.backend.api.admin.dto.response;

import com.bp20.backend.api.user.domain.User;
import com.bp20.backend.api.user.domain.UserRole;
import com.bp20.backend.api.user.domain.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "관리자 응답")
public record AdminAccountResponse(
        @Schema(description = "관리자 ID", example = "1")
        Long id,
        @Schema(description = "관리자 이메일", example = "admin@bp20.com")
        String email,
        @Schema(description = "관리자 이름", example = "김관리")
        String name,
        @Schema(description = "관리자 연락처", example = "010-1234-5678")
        String phoneNumber,
        @Schema(description = "관리자 역할", example = "ADMIN")
        UserRole role,
        @Schema(description = "사용자 상태", example = "ACTIVE")
        UserStatus status,
        @Schema(description = "생성 일시")
        LocalDateTime createdAt,
        @Schema(description = "수정 일시")
        LocalDateTime updatedAt
) {
    public static AdminAccountResponse from(User user) {
        return new AdminAccountResponse(
                user.getId(), user.getEmail(), user.getName(), user.getPhoneNumber(),
                user.getRole(), user.getStatus(), user.getCreatedAt(), user.getUpdatedAt()
        );
    }
}
