package com.bp20.backend.api.ai.dto.request;

import jakarta.validation.constraints.NotNull;

public record AgentRunResumeRequest(
        @NotNull Decision decision,
        String modificationPlan,
        String selectedAction
) {
    public enum Decision {
        approve, edit, reject
    }
}
