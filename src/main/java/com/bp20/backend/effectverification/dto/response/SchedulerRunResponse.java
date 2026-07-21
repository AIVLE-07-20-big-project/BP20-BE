package com.bp20.backend.effectverification.dto.response;

public record SchedulerRunResponse(
        int processed,
        int succeeded,
        int failed,
        int skipped
) {
}
