package com.bp20.backend.api.iam.admin.dto.response;

import com.bp20.backend.api.user.domain.User;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "재인증 후 일시 공개되는 관리자 개인정보 원문")
public record AdminPersonalDataResponse(
        Long id,
        String email,
        String name,
        String phoneNumber,
        Instant visibleUntil
) {
    public static AdminPersonalDataResponse from(User user, Instant visibleUntil) {
        return new AdminPersonalDataResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getPhoneNumber(),
                visibleUntil
        );
    }
}
