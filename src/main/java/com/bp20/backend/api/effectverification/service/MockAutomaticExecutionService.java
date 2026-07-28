package com.bp20.backend.api.effectverification.service;

import com.bp20.backend.api.effectverification.collector.VerificationMetricCollector;
import com.bp20.backend.api.effectverification.dto.request.ExecutionRegistrationRequest;
import com.bp20.backend.api.effectverification.dto.request.PeriodMetrics;
import com.bp20.backend.api.effectverification.dto.request.RecommendationType;
import com.bp20.backend.api.effectverification.dto.request.VerificationCompletionRequest;
import com.bp20.backend.api.effectverification.dto.request.VerificationCondition;
import com.bp20.backend.api.effectverification.dto.response.EffectVerificationResponse;
import com.bp20.backend.api.effectverification.dto.response.VerificationExecutionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

@Service
@Profile("mock")
@RequiredArgsConstructor
public class MockAutomaticExecutionService {

    private static final int VERIFICATION_PERIOD_DAYS = 14;

    private final JdbcTemplate jdbcTemplate;
    private final VerificationMetricCollector metricCollector;
    private final MockReviewSentimentService reviewSentimentService;
    private final EffectVerificationLifecycleService lifecycleService;
    private final MockRecommendationFeedbackService feedbackService;

    public VerificationExecutionResponse registerAutomatically(Long recommendationId) {
        MockRecommendation recommendation = findRecommendation(recommendationId);
        validateExecuted(recommendation);

        LocalDateTime baselineFrom = recommendation.executedAt()
                .minusDays(VERIFICATION_PERIOD_DAYS);
        LocalDateTime baselineTo = recommendation.executedAt();
        VerificationCondition condition = conditionOf(recommendation);

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
        return lifecycleService.registerExecution(null, request);
    }

    public EffectVerificationResponse completeAutomatically(Long recommendationId) {
        MockRecommendation recommendation = findRecommendation(recommendationId);
        validateExecuted(recommendation);

        LocalDateTime collectionFrom = recommendation.executedAt();
        LocalDateTime collectionTo = recommendation.executedAt()
                .plusDays(VERIFICATION_PERIOD_DAYS);
        VerificationCondition condition = conditionOf(recommendation);

        if (recommendation.type() == RecommendationType.REVIEW) {
            reviewSentimentService.analyzePending(
                    recommendation.storeId(),
                    collectionFrom,
                    collectionTo
            );
        }

        PeriodMetrics after = metricCollector.collect(
                recommendation.storeId(),
                recommendation.type(),
                collectionFrom,
                collectionTo,
                condition
        );
        VerificationCompletionRequest request = new VerificationCompletionRequest();
        request.setAfter(after);
        request.setCollectedAt(collectionTo);
        EffectVerificationResponse response = lifecycleService
                .completeVerification(null, recommendationId, request);
        feedbackService.apply(recommendationId, response);
        return response;
    }

    public boolean supportsRecommendation(Long recommendationId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM MockRecommendation WHERE RecommendationID = ?",
                Integer.class,
                recommendationId
        );
        return count != null && count > 0;
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
                    (resultSet, rowNumber) -> mapRecommendation(resultSet),
                    recommendationId
            );
        } catch (EmptyResultDataAccessException exception) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Mock recommendation not found"
            );
        }
    }

    private MockRecommendation mapRecommendation(ResultSet resultSet)
            throws SQLException {
        Timestamp executedAt = resultSet.getTimestamp("ExecutedAt");
        return new MockRecommendation(
                resultSet.getLong("RecommendationID"),
                resultSet.getLong("StoreID"),
                RecommendationType.valueOf(
                        resultSet.getString("RecommendationType")
                ),
                (Integer) resultSet.getObject("TargetStartHour"),
                (Integer) resultSet.getObject("TargetEndHour"),
                resultSet.getString("TargetAspect"),
                resultSet.getBoolean("Executed"),
                executedAt == null ? null : executedAt.toLocalDateTime()
        );
    }

    private VerificationCondition conditionOf(MockRecommendation recommendation) {
        return new VerificationCondition(
                VERIFICATION_PERIOD_DAYS,
                recommendation.targetStartHour(),
                recommendation.targetEndHour(),
                true,
                recommendation.targetAspect()
        );
    }

    private void validateExecuted(MockRecommendation recommendation) {
        if (!recommendation.executed() || recommendation.executedAt() == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Recommendation has not been executed"
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
