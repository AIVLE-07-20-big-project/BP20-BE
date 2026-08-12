package com.bp20.backend.api.commerce.order.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "온라인 결제 완료 이력 등록 요청")
public record CreateOnlinePurchaseRequest(
        @NotNull(message = "고객 ID는 필수입니다.")
        @Schema(description = "구매 고객 ID", example = "1")
        Long customerId,

        @PastOrPresent(message = "구매 일시는 현재보다 이후일 수 없습니다.")
        @Schema(description = "구매 완료 일시. 생략하면 현재 시각", example = "2026-08-05T14:30:00")
        LocalDateTime purchasedAt,

        @NotEmpty(message = "구매 상품은 한 개 이상이어야 합니다.")
        List<@Valid Item> items
) {
    public record Item(
            @NotNull(message = "상품 ID는 필수입니다.")
            @Schema(description = "온라인 판매 상품 ID", example = "1")
            Long productId,

            @Positive(message = "구매 수량은 1개 이상이어야 합니다.")
            @Schema(description = "구매 수량", example = "2")
            int quantity
    ) {
    }
}
