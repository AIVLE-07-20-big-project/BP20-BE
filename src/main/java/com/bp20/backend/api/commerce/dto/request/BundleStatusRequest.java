package com.bp20.backend.api.commerce.dto.request;

import com.bp20.backend.api.commerce.domain.BundleStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "세트상품 상태 변경 요청")
public record BundleStatusRequest(
        @Schema(
                description = "변경할 세트상품 상태",
                example = "ON_SALE",
                allowableValues = {"DRAFT", "ON_SALE", "SOLD_OUT", "HIDDEN"}
        )
        @NotNull(message = "세트상품 상태는 필수입니다.")
        BundleStatus status
) {
}
