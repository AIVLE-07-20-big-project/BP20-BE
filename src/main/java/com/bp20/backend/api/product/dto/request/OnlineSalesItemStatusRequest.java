package com.bp20.backend.api.product.dto.request;

import com.bp20.backend.api.product.domain.OnlineSalesItemStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "온라인 판매 항목 상태 변경 요청")
public record OnlineSalesItemStatusRequest(
        @NotNull(message = "온라인 판매 상태는 필수입니다.")
        @Schema(
                description = "온라인 채널에서의 상품 또는 세트상품 상태",
                example = "ON_SALE",
                allowableValues = {"ON_SALE", "SOLD_OUT", "HIDDEN"}
        )
        OnlineSalesItemStatus status
) {
}
