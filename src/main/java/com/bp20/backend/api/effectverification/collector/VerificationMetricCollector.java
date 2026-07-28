package com.bp20.backend.api.effectverification.collector;

import com.bp20.backend.api.effectverification.dto.request.PeriodMetrics;
import com.bp20.backend.api.effectverification.dto.request.RecommendationType;
import com.bp20.backend.api.effectverification.dto.request.VerificationCondition;

import java.time.LocalDateTime;

public interface VerificationMetricCollector {

    PeriodMetrics collect(
            Long storeId,
            RecommendationType recommendationType,
            LocalDateTime from,
            LocalDateTime to,
            VerificationCondition condition
    );
}

