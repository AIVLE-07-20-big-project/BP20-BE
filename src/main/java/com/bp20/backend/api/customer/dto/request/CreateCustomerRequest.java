package com.bp20.backend.api.customer.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "고객 등록 요청")
public record CreateCustomerRequest(
        @Schema(description = "고객 이메일", example = "customer@bp20.com")
        @NotBlank(message = "고객 이메일은 필수입니다.")
        @Email(message = "고객 이메일 형식이 올바르지 않습니다.")
        @Size(max = 100, message = "고객 이메일은 100자 이하여야 합니다.")
        String email,

        @Schema(description = "고객 이름", example = "김고객")
        @NotBlank(message = "고객 이름은 필수입니다.")
        @Size(max = 50, message = "고객 이름은 50자 이하여야 합니다.")
        String name,

        @Schema(description = "고객 전화번호", example = "010-1234-5678")
        @Pattern(
                regexp = "^$|^[0-9+()\\-\\s]{8,30}$",
                message = "고객 전화번호 형식이 올바르지 않습니다."
        )
        String phoneNumber
) {
}
