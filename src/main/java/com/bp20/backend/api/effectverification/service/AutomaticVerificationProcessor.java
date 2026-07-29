package com.bp20.backend.api.effectverification.service;

import com.bp20.backend.api.effectverification.collector.VerificationMetricCollector;
import com.bp20.backend.api.effectverification.domain.EffectVerificationExecution;
import com.bp20.backend.api.effectverification.dto.request.PeriodMetrics;
import com.bp20.backend.api.effectverification.dto.request.VerificationCompletionRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AutomaticVerificationProcessor {

    private final ObjectProvider<VerificationMetricCollector> metricCollectorProvider;
    private final EffectVerificationLifecycleService lifecycleService;

    public boolean isAvailable() {
        return metricCollectorProvider.getIfAvailable() != null;
    }

    public void process(EffectVerificationExecution execution) {
        VerificationMetricCollector collector = metricCollectorProvider.getIfAvailable();
        if (collector == null) {
            throw new IllegalStateException(
                    "Verification metric collector is not configured"
            );
        }

        var context = lifecycleService.getAutomaticCollectionContext(
                execution.getAiRecommendationId()
        );
        PeriodMetrics after = collector.collect(
                context.storeId(),
                context.recommendationType(),
                context.collectionFrom(),
                context.collectionTo(),
                context.condition()
        );

        VerificationCompletionRequest request = new VerificationCompletionRequest();
        request.setAfter(after);
        request.setCollectedAt(context.collectionTo());
        lifecycleService.completeVerification(
                null,
                execution.getAiRecommendationId(),
                request
        );
    }
}
