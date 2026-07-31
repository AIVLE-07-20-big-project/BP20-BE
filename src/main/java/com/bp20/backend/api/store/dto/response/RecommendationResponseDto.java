package com.bp20.backend.api.store.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record RecommendationResponseDto(
        @JsonProperty("store_id") Long storeId,
        @JsonProperty("improvement_report") ImprovementReportDto improvementReport
) {
    public record ImprovementReportDto(
            @JsonProperty("executive_summary") String executiveSummary,
            @JsonProperty("action_items") List<ActionItemDto> actionItems
    ) {}

    public record ActionItemDto(
            String priority,
            String aspect,
            String keyword,
            @JsonProperty("trend_summary") String trendSummary,
            @JsonProperty("problem_cause") String problemCause,
            @JsonProperty("action_plan") String actionPlan,
            @JsonProperty("expected_outcome") String expectedOutcome
    ) {}
}