package com.bp20.backend.api.ai.dto.request;

import jakarta.validation.constraints.NotNull;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public record AgentRunResumeRequest(
        @NotNull Decision decision,
        String modificationPlan,
        String selectedAction,
        @JsonProperty("execution_started_at") LocalDateTime executionStartedAt,
        @JsonProperty("execution_ended_at") LocalDateTime executionEndedAt
) {
    public AgentRunResumeRequest(Decision decision, String modificationPlan) {
        this(decision, modificationPlan, null, null, null);
    }

    public AgentRunResumeRequest(Decision decision, String modificationPlan, String selectedAction) {
        this(decision, modificationPlan, selectedAction, null, null);
    }

    public enum Decision {
        approve, edit, reject
    }
}
