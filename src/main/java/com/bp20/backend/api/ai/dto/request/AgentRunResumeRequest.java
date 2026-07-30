package com.bp20.backend.api.ai.dto.request;

import jakarta.validation.constraints.NotNull;

public record AgentRunResumeRequest(
        @NotNull Decision decision,
        String modificationPlan,
        String selectedAction
) {
    public AgentRunResumeRequest(Decision decision, String modificationPlan) {
        this(decision, modificationPlan, null);
    }

    public enum Decision {
        approve, edit, reject
    }
}
