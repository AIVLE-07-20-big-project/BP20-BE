package com.bp20.backend.api.commerce.dto.request;

import com.bp20.backend.api.commerce.domain.DiscountType;
import com.bp20.backend.api.commerce.domain.CouponUsageChannel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

@Schema(description = "고객 쿠폰 발급 요청")
public record IssueCouponRequest(
        @Schema(description = "쿠폰을 받을 고객 ID", example = "1")
        @NotNull(message = "고객 ID는 필수입니다.")
        Long customerId,

        @Schema(description = "쿠폰명", example = "신규 고객 3,000원 쿠폰")
        @NotBlank(message = "쿠폰명은 필수입니다.")
        @Size(max = 120, message = "쿠폰명은 120자 이하여야 합니다.")
        String name,

        @Schema(description = "할인 계산 유형", example = "FIXED_AMOUNT", allowableValues = {"RATE", "FIXED_AMOUNT"})
        @NotNull(message = "할인 유형은 필수입니다.")
        DiscountType discountType,

        @Schema(description = "정률 할인율 또는 정액 할인 금액", example = "3000")
        @Positive(message = "할인 값은 0보다 커야 합니다.")
        long discountValue,

        @Schema(description = "쿠폰 만료 일시", example = "2026-08-31T23:59:59")
        @NotNull(message = "쿠폰 만료 시각은 필수입니다.")
        @Future(message = "쿠폰 만료 시각은 현재보다 이후여야 합니다.")
        LocalDateTime expiresAt,

        @Schema(
                description = "쿠폰 사용 채널. 온라인 전용 또는 오프라인 전용 중 하나를 선택합니다.",
                example = "OFFLINE_ONLY",
                allowableValues = {"ONLINE_ONLY", "OFFLINE_ONLY"}
        )
        @NotNull(message = "쿠폰 사용 채널은 필수입니다.")
        CouponUsageChannel usageChannel,

        @Schema(description = "매장 방문 쿠폰 발급의 근거가 된 온라인 구매 이력 ID", example = "1")
        Long sourceOnlinePurchaseId
) {
}
