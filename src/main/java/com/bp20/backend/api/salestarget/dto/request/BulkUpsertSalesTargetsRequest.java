package com.bp20.backend.api.salestarget.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;

import java.util.List;

public record BulkUpsertSalesTargetsRequest(
        @NotNull String sourceBatchId,
        @NotEmpty @Valid List<SalesTargetItemRequest> items
) {
}
