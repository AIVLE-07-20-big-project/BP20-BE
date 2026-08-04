package com.bp20.backend.api.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "액세스 토큰 재발급 응답")
public record TokenRefreshResponse(
        @Schema(description = "새 JWT 액세스 토큰")
        String accessToken,

        @Schema(description = "토큰 유형", example = "Bearer")
        String tokenType
) {
    public static TokenRefreshResponse of(String accessToken) {
        return new TokenRefreshResponse(accessToken, "Bearer");
    }
}
