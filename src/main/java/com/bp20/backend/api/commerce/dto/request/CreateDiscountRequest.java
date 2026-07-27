package com.bp20.backend.api.commerce.dto.request;

import com.bp20.backend.api.commerce.domain.DiscountType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Schema(description = "온·오프라인 공통 할인 등록 요청")
public record CreateDiscountRequest(
        @Schema(description = "할인명", example = "오후 2~5시 아메리카노 할인")
        @NotBlank(message = "할인명은 필수입니다.")
        @Size(max = 120, message = "할인명은 120자 이하여야 합니다.")
        String name,

        @Schema(description = "할인 설명", example = "비수기 시간대에 아메리카노를 할인합니다.")
        @Size(max = 1000, message = "할인 설명은 1,000자 이하여야 합니다.")
        String description,

        @Schema(description = "할인 계산 유형", example = "RATE", allowableValues = {"RATE", "FIXED_AMOUNT"})
        @NotNull(message = "할인 유형은 필수입니다.")
        DiscountType discountType,

        @Schema(description = "정률 할인율 또는 정액 할인 금액", example = "15")
        @Positive(message = "할인 값은 0보다 커야 합니다.")
        long discountValue,

        @Schema(description = "할인 대상 상품 ID", example = "1")
        @NotNull(message = "상품 ID는 필수입니다.")
        Long productId,

        @Schema(description = "할인 시작 일시", example = "2026-08-01T14:00:00")
        @NotNull(message = "할인 시작 시각은 필수입니다.")
        LocalDateTime startsAt,

        @Schema(description = "할인 종료 일시", example = "2026-08-31T17:00:00")
        @NotNull(message = "할인 종료 시각은 필수입니다.")
        LocalDateTime endsAt,

        @Schema(description = "매일 할인 적용 시작 시각", example = "14:00:00")
        LocalTime dailyStartTime,

        @Schema(description = "매일 할인 적용 종료 시각", example = "17:00:00")
        LocalTime dailyEndTime,

        @Schema(description = "할인 리마인드 알림 사용 여부", example = "true")
        boolean reminderEnabled
) {
}
