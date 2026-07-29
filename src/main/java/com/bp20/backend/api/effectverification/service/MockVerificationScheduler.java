package com.bp20.backend.api.effectverification.service;

import com.bp20.backend.api.effectverification.dto.response.SchedulerRunResponse;
import com.bp20.backend.api.effectverification.domain.EffectVerificationExecution;
import com.bp20.backend.api.effectverification.domain.VerificationStatus;
import com.bp20.backend.api.effectverification.repository.EffectVerificationExecutionRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Profile("mock")
@RequiredArgsConstructor
public class MockVerificationScheduler {

    private static final Logger log = LoggerFactory.getLogger(
            MockVerificationScheduler.class
    );

    private final EffectVerificationExecutionRepository executionRepository;
    private final MockAutomaticExecutionService automaticExecutionService;

    @Value("${effect-verification.scheduler.enabled:false}")
    private boolean enabled;

    @Value("${effect-verification.scheduler.max-attempts:3}")
    private int maxAttempts;

    @Scheduled(
            fixedDelayString = "${effect-verification.scheduler.fixed-delay-ms:60000}"
    )
    public void runScheduled() {
        if (enabled) {
            runDueVerifications();
        }
    }

    public SchedulerRunResponse runDueVerifications() {
        List<EffectVerificationExecution> executions = executionRepository
                .findByStatusInAndVerificationDueAtLessThanEqualOrderByVerificationDueAtAsc(
                        List.of(
                                VerificationStatus.COLLECTING,
                                VerificationStatus.READY,
                                VerificationStatus.FAILED
                        ),
                        LocalDateTime.now()
                );

        int succeeded = 0;
        int failed = 0;
        int skipped = 0;
        for (EffectVerificationExecution execution : executions) {
            int attemptCount = execution.getAttemptCount() == null
                    ? 0
                    : execution.getAttemptCount();
            if (attemptCount >= maxAttempts) {
                skipped++;
                continue;
            }
            try {
                String recommendationId = execution.getAiRecommendationId();
                Long mockRecommendationId = numericId(recommendationId);
                if (mockRecommendationId != null) {
                    if (!automaticExecutionService.supportsRecommendation(
                            mockRecommendationId
                    )) {
                        skipped++;
                        continue;
                    }
                    automaticExecutionService.completeAutomatically(
                            mockRecommendationId
                    );
                } else if (recommendationId.startsWith("mock-")) {
                    automaticExecutionService.completeThreadAutomatically(
                            recommendationId
                    );
                } else {
                    skipped++;
                    continue;
                }
                succeeded++;
            } catch (RuntimeException exception) {
                failed++;
                log.warn(
                        "Automatic verification failed for recommendation {}: {}",
                        execution.getAiRecommendationId(),
                        exception.getMessage()
                );
            }
        }
        return new SchedulerRunResponse(
                executions.size(),
                succeeded,
                failed,
                skipped
        );
    }

    private Long numericId(String recommendationId) {
        try {
            return Long.valueOf(recommendationId);
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
