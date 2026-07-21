package com.bp20.backend.effectverification.service;

import com.bp20.backend.effectverification.collector.VerificationMetricCollector;
import com.bp20.backend.effectverification.dto.request.ExecutionRegistrationRequest;
import com.bp20.backend.effectverification.dto.request.PeriodMetrics;
import com.bp20.backend.effectverification.dto.request.RecommendationType;
import com.bp20.backend.effectverification.dto.request.VerificationCondition;
import com.bp20.backend.effectverification.dto.response.VerificationExecutionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
@Profile("mock")
@RequiredArgsConstructor
public class MockAutomaticExecutionService {

    private static final int BASELINE_PERIOD_DAYS = 14;

    private final JdbcTemplate jdbcTemplate;
    private final VerificationMetricCollector metricCollector;
    private final MockReviewSentimentService reviewSentimentService;
    private final EffectVerificationLifecycleService lifecycleService;

    public VerificationExecutionResponse registerAutomatically(Long recommendationId) {
        MockRecommendation recommendation = findRecommendation(recommendationId);
        if (!recommendation.executed() || recommendation.executedAt() == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Recommendation has not been executed"
            );
        }

        LocalDateTime baselineFrom = recommendation.executedAt()
                .minusDays(BASELINE_PERIOD_DAYS);
        LocalDateTime baselineTo = recommendation.executedAt();
        VerificationCondition condition = new VerificationCondition(
                BASELINE_PERIOD_DAYS,
                recommendation.targetStartHour(),
                recommendation.targetEndHour(),
                true,
                recommendation.targetAspect()
        );

        if (recommendation.type() == RecommendationType.REVIEW) {
            reviewSentimentService.analyzePending(
                    recommendation.storeId(),
                    baselineFrom,
                    baselineTo
            );
        }

        PeriodMetrics before = metricCollector.collect(
                recommendation.storeId(),
                recommendation.type(),
                baselineFrom,
                baselineTo,
                condition
        );

        ExecutionRegistrationRequest request = new ExecutionRegistrationRequest();
        request.setStoreId(recommendation.storeId());
        request.setRecommendationId(recommendation.recommendationId());
        request.setRecommendationType(recommendation.type());
        request.setCondition(condition);
        request.setBefore(before);
        request.setExecutedAt(recommendation.executedAt());
        return lifecycleService.registerExecution(request);
    }

    private MockRecommendation findRecommendation(Long recommendationId) {
        try {
            return jdbcTemplate.queryForObject(
                    """
                    SELECT RecommendationID, StoreID, RecommendationType,
                           TargetStartHour, TargetEndHour, TargetAspect,
                           Executed, ExecutedAt
                    FROM MockRecommendation
                    WHERE RecommendationID = ?
                    """,
                    (resultSet, rowNumber) -> new MockRecommendation(
                            resultSet.getLong("RecommendationID"),
                            resultSet.getLong("StoreID"),
                            RecommendationType.valueOf(
                                    resultSet.getString("RecommendationType")
                            ),
                            (Integer) resultSet.getObject("TargetStartHour"),
                            (Integer) resultSet.getObject("TargetEndHour"),
                            resultSet.getString("TargetAspect"),
                            resultSet.getBoolean("Executed"),
                            resultSet.getTimestamp("ExecutedAt") == null
                                    ? null
                                    : resultSet.getTimestamp("ExecutedAt")
                                    .toLocalDateTime()
                    ),
                    recommendationId
            );
        } catch (EmptyResultDataAccessException exception) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Mock recommendation not found"
            );
        }
    }

    record MockRecommendation(
            Long recommendationId,
            Long storeId,
            RecommendationType type,
            Integer targetStartHour,
            Integer targetEndHour,
            String targetAspect,
            boolean executed,
            LocalDateTime executedAt
    ) {
    }
}

