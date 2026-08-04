package com.bp20.backend.api.store.admin.dto;

import com.bp20.backend.api.user.domain.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "매장 모니터링 응답")
public record MerchantMonitoringResponse(
        long totalMerchants,
        long activeMerchants,
        long aiActiveMerchants,
        List<Merchant> merchants
) {
    @Schema(description = "매장별 모니터링 정보")
    public record Merchant(
            Long storeId,
            String storeName,
            String category,
            String address,
            Long ownerId,
            String ownerName,
            String ownerEmail,
            UserStatus ownerStatus,
            LocalDateTime createdAt,
            long analysisCount,
            long recommendationRunCount,
            long executedRecommendationCount,
            boolean aiActive
    ) {
    }
}
