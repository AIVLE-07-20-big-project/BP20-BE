package com.bp20.backend.api.effectverification.dto.response;

import com.bp20.backend.api.effectverification.dto.request.SelectedActionRequest;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.Map;

public record MockVerificationCandidateResponse(
        @JsonProperty("thread_id")
        String threadId,
        @JsonProperty("store_id")
        Long storeId,
        @JsonProperty("approval_status")
        String approvalStatus,
        @JsonProperty("selected_action")
        SelectedActionRequest selectedAction,
        @JsonProperty("final_report")
        Map<String, Object> finalReport,
        @JsonProperty("created_at")
        LocalDateTime createdAt,
        boolean mock
) {
}
