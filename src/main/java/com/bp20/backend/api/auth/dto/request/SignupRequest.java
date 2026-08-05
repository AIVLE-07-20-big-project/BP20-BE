package com.bp20.backend.api.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import com.bp20.backend.api.user.privacy.PrivacyPolicy;

@Schema(description = "초대 기반 회원가입 요청")
public record SignupRequest(
        @Schema(description = "초대받은 이메일", example = "admin@bp20.com")
        @Email @NotBlank @Size(max = 100)
        String email,

        @Schema(description = "초대 시 한 번만 발급되는 일회용 임시 비밀번호", example = "A7!kP3@mN8#qR5$t")
        @NotBlank @Size(max = 64)
        String temporaryPassword,

        @Schema(description = "12~72자이며 영문 대문자·소문자·숫자·특수문자 중 3종 이상을 포함하는 새 비밀번호", example = "Bp20Admin!001")
        @NotBlank @Size(min = 12, max = 72)
        String password,

        @Schema(description = "사용자 이름", example = "BP20 관리자")
        @NotBlank @Size(max = 50)
        String name,

        @Schema(description = "사용자 연락처", example = "010-1234-5678")
        @Size(max = 30)
        String phoneNumber,

        @Schema(description = "필수 개인정보 수집 및 이용 동의", example = "true")
        boolean privacyConsent,

        @Schema(description = "동의한 개인정보 처리방침 버전", example = "2026-08-03")
        @NotBlank @Size(max = 20)
        String privacyPolicyVersion,

        @Schema(description = "Google reCAPTCHA v3 회원가입 검증 토큰")
        String captchaToken
) {
    public SignupRequest(
            String email,
            String temporaryPassword,
            String password,
            String name,
            String phoneNumber
    ) {
        this(email, temporaryPassword, password, name, phoneNumber, true, PrivacyPolicy.CURRENT_VERSION, null);
    }
}
