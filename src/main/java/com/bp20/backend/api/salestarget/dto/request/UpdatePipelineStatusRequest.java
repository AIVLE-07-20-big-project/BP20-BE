package com.bp20.backend.api.salestarget.dto.request;

import com.bp20.backend.api.salestarget.domain.PipelineStatus;
import jakarta.validation.constraints.NotNull;

public record UpdatePipelineStatusRequest(
        @NotNull PipelineStatus pipelineStatus
) {
}
