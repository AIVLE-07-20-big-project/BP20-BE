package com.bp20.backend.api.effectverification.service;

import com.bp20.backend.api.effectverification.domain.EffectVerificationExecution;
import com.bp20.backend.api.effectverification.domain.VerificationStatus;
import com.bp20.backend.api.effectverification.dto.response.VerificationExecutionResponse;
import com.bp20.backend.api.effectverification.repository.EffectVerificationExecutionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class EffectVerificationRetryService {

    private final EffectVerificationExecutionRepository executionRepository;
    private final AutomaticVerificationProcessor processor;
    private final EffectVerificationLifecycleService lifecycleService;

    @Value("${effect-verification.scheduler.max-attempts:3}")
    private int maxAttempts;

    @Transactional
    public VerificationExecutionResponse retry(Long userId, String recommendationId) {
        EffectVerificationExecution execution = (userId == null
                ? executionRepository.findByAiRecommendationId(recommendationId)
                : executionRepository.findByAiRecommendationIdAndUserId(
                        recommendationId,
                        userId
                ))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Verification execution not found"
                ));

        if (execution.getStatus() != VerificationStatus.FAILED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only failed verification can be retried"
            );
        }
        int attempts = execution.getAttemptCount() == null
                ? 0
                : execution.getAttemptCount();
        if (attempts >= maxAttempts) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Maximum verification attempts exceeded"
            );
        }
        if (!processor.isAvailable()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Verification metric collector is not configured"
            );
        }

        processor.process(execution);
        return lifecycleService.getExecution(userId, recommendationId);
    }
}
