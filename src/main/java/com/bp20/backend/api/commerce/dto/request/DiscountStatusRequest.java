package com.bp20.backend.api.commerce.dto.request;

import com.bp20.backend.api.commerce.domain.DiscountStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "할인 상태 변경 요청")
public record DiscountStatusRequest(
        @Schema(
                description = "변경할 할인 상태",
                example = "ACTIVE",
                allowableValues = {"SCHEDULED", "ACTIVE", "PAUSED", "ENDED"}
        )
        @NotNull(message = "할인 상태는 필수입니다.")
        DiscountStatus status
) {
}
