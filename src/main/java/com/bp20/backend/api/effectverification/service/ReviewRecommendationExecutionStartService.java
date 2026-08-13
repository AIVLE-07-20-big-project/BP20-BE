package com.bp20.backend.api.effectverification.service;

import com.bp20.backend.api.effectverification.collector.VerificationMetricCollector;
import com.bp20.backend.api.effectverification.dto.request.ExecutionRegistrationRequest;
import com.bp20.backend.api.effectverification.dto.request.PeriodMetrics;
import com.bp20.backend.api.effectverification.dto.request.RecommendationType;
import com.bp20.backend.api.effectverification.dto.request.SelectedActionRequest;
import com.bp20.backend.api.effectverification.dto.request.VerificationCondition;
import com.bp20.backend.api.effectverification.dto.response.VerificationExecutionResponse;
import com.bp20.backend.api.effectverification.repository.EffectVerificationExecutionRepository;
import com.bp20.backend.api.store.service.StoreReviewRecommendationService.CompletedAction;
import com.bp20.backend.api.store.service.StoreReviewRecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ReviewRecommendationExecutionStartService {

    private static final int DEFAULT_PERIOD_DAYS = 30;

    private final ObjectProvider<VerificationMetricCollector> metricCollectorProvider;
    private final EffectVerificationExecutionRepository executionRepository;
    private final EffectVerificationLifecycleService lifecycleService;
    private final EffectVerificationStoreAccessService storeAccessService;
    private final StoreReviewRecommendationService reviewRecommendationService;

    @Transactional
    public VerificationExecutionResponse startAction(
            Long userId,
            Long recommendationId,
            String keyword
    ) {
        return start(
                userId,
                reviewRecommendationService.completeActionItem(
                        userId,
                        recommendationId,
                        keyword
                )
        );
    }

    private VerificationExecutionResponse start(Long userId, CompletedAction action) {
        Long storeId = storeAccessService.resolveOwnedStoreId(
                userId,
                action.storeId()
        );
        if (executionRepository.existsByAiRecommendationId(
                action.verificationRecommendationId()
        )) {
            return lifecycleService.getExecution(
                    userId,
                    action.verificationRecommendationId()
            );
        }

        VerificationMetricCollector metricCollector =
                metricCollectorProvider.getIfAvailable();
        if (metricCollector == null) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Verification metric collector is not configured"
            );
        }

        VerificationCondition condition = new VerificationCondition(
                DEFAULT_PERIOD_DAYS,
                null,
                null,
                false,
                action.targetAspect()
        );
        PeriodMetrics before = metricCollector.collect(
                storeId,
                RecommendationType.REVIEW,
                action.executedAt().minusDays(DEFAULT_PERIOD_DAYS),
                action.executedAt(),
                condition
        );

        ExecutionRegistrationRequest request = new ExecutionRegistrationRequest();
        request.setRecommendationId(action.verificationRecommendationId());
        request.setStoreId(storeId);
        request.setRecommendationType(RecommendationType.REVIEW);
        request.setCondition(condition);
        request.setBefore(before);
        request.setExecutedAt(action.executedAt());
        SelectedActionRequest selectedAction = new SelectedActionRequest();
        selectedAction.setAction(action.actionPlan());
        request.setSelectedAction(selectedAction);

        return lifecycleService.registerExecution(userId, request);
    }
}
