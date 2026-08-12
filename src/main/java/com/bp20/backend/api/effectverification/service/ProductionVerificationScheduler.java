package com.bp20.backend.api.effectverification.service;

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
@Profile("!mock")
@RequiredArgsConstructor
public class ProductionVerificationScheduler {

    private static final Logger log = LoggerFactory.getLogger(
            ProductionVerificationScheduler.class
    );

    private final EffectVerificationExecutionRepository executionRepository;
    private final AutomaticVerificationProcessor processor;
    private final EffectVerificationSchedulerLock schedulerLock;

    @Value("${effect-verification.scheduler.enabled:true}")
    private boolean enabled;

    @Value("${effect-verification.scheduler.max-attempts:3}")
    private int maxAttempts;

    @Scheduled(
            cron = "${effect-verification.scheduler.cron:0 0 2 * * *}",
            zone = "${effect-verification.scheduler.zone:Asia/Seoul}"
    )
    public void runScheduled() {
        if (!enabled || !processor.isAvailable()) {
            return;
        }

        boolean acquired = schedulerLock.runWithLock(this::processDueExecutions);
        if (!acquired) {
            log.debug("Effect verification scheduler skipped because another task owns the lock");
        }
    }

    private void processDueExecutions() {
        List<EffectVerificationExecution> executions = executionRepository
                .findByStatusInAndVerificationDueAtLessThanEqualOrderByVerificationDueAtAsc(
                        List.of(
                                VerificationStatus.COLLECTING,
                                VerificationStatus.READY,
                                VerificationStatus.FAILED
                        ),
                        LocalDateTime.now()
                );

        for (EffectVerificationExecution execution : executions) {
            int attempts = execution.getAttemptCount() == null
                    ? 0
                    : execution.getAttemptCount();
            if (attempts >= maxAttempts) {
                continue;
            }
            try {
                processor.process(execution);
            } catch (RuntimeException exception) {
                log.warn(
                        "Automatic effect verification failed for {}: {}",
                        execution.getAiRecommendationId(),
                        exception.getMessage()
                );
            }
        }
    }
}
