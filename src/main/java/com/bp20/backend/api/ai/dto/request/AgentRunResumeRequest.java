package com.bp20.backend.api.ai.dto.request;

import jakarta.validation.constraints.NotNull;

public record AgentRunResumeRequest(
        @NotNull Decision decision,
        String modificationPlan
) {
    public enum Decision {
        approve, edit, reject
    }
}
