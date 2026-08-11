package com.bp20.backend.api.review.dto.response;

public record MonthlyReportStatusResponseDto(
        String targetMonth,
        boolean generated
) {}
