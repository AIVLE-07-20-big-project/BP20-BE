package com.bp20.backend.api.store.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "점주 매장 등록 요청")
public record CreateStoreRequest(
        @Schema(description = "매장명", example = "성수 브루랩")
        @NotBlank(message = "매장명은 필수입니다.")
        @Size(max = 100, message = "매장명은 100자 이하여야 합니다.")
        String name,

        @Schema(description = "사업자등록번호. 하이픈은 생략할 수 있습니다.", example = "123-45-67890")
        @NotBlank(message = "사업자등록번호는 필수입니다.")
        @Pattern(regexp = "^\\d{3}-?\\d{2}-?\\d{5}$", message = "사업자등록번호 형식이 올바르지 않습니다.")
        String businessNumber,

        @Schema(description = "매장 업종", example = "카페")
        @NotBlank(message = "업종은 필수입니다.")
        @Size(max = 50, message = "업종은 50자 이하여야 합니다.")
        String category,

        @Schema(description = "매장 주소", example = "서울특별시 성동구 성수이로 20")
        @NotBlank(message = "주소는 필수입니다.")
        @Size(max = 255, message = "주소는 255자 이하여야 합니다.")
        String address,

        @Schema(description = "매장 전화번호", example = "02-1234-5678")
        @Size(max = 20, message = "전화번호는 20자 이하여야 합니다.")
        String phoneNumber
) {
}
