package com.bp20.backend.api.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "점주 초대 수락 요청")
public record AcceptStoreOwnerInvitationRequest(
        @Schema(description = "초대받은 점주 이메일", example = "store-owner@bp20.com")
        @Email @NotBlank @Size(max = 100)
        String email,

        @Schema(description = "초대 시 한 번만 발급되는 임시 비밀번호", example = "A7!kP3@mN8#qR5$t")
        @NotBlank @Size(max = 64)
        String temporaryPassword,

        @Schema(description = "점주가 UI에서 새로 설정할 비밀번호", example = "Passw0rd!23")
        @NotBlank @Size(min = 8, max = 72)
        String newPassword,

        @Schema(description = "점주 이름", example = "홍길동")
        @NotBlank @Size(max = 50)
        String name,

        @Schema(description = "점주 연락처", example = "010-1234-5678")
        @Size(max = 30)
        String phoneNumber
) {
}
