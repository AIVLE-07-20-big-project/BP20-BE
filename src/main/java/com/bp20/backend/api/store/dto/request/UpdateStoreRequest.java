package com.bp20.backend.api.store.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "점주 매장 수정 요청")
public record UpdateStoreRequest(
        @Schema(description = "변경할 매장명", example = "성수 브루랩 본점")
        @NotBlank(message = "매장명은 필수입니다.")
        @Size(max = 100, message = "매장명은 100자 이하여야 합니다.")
        String name,

        @Schema(description = "변경할 매장 업종", example = "카페·베이커리")
        @NotBlank(message = "업종은 필수입니다.")
        @Size(max = 50, message = "업종은 50자 이하여야 합니다.")
        String category,

        @Schema(description = "변경할 매장 주소", example = "서울특별시 성동구 연무장길 10")
        @NotBlank(message = "주소는 필수입니다.")
        @Size(max = 255, message = "주소는 255자 이하여야 합니다.")
        String address,

        @Schema(description = "변경할 매장 전화번호", example = "02-9876-5432")
        @Size(max = 20, message = "전화번호는 20자 이하여야 합니다.")
        String phoneNumber
) {
}
