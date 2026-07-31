package com.bp20.backend.api.salestarget.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SalesTargetItemRequest(
        @NotBlank String businessName,
        String industry,
        @NotBlank String address,
        @NotNull Double totalScore,
        Double growthScore,
        Double trafficScore,
        Double reviewScore,
        Double similarityScore
) {
}
