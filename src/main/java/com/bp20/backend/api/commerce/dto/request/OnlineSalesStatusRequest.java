package com.bp20.backend.api.commerce.dto.request;

import com.bp20.backend.api.store.domain.OnlineSalesStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "온라인 판매 상태 변경 요청")
public record OnlineSalesStatusRequest(
        @NotNull
        @Schema(description = "온라인 판매 상태", example = "OPEN")
        OnlineSalesStatus status
) {
}
