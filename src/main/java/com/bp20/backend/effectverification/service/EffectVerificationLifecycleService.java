package com.bp20.backend.effectverification.service;

import com.bp20.backend.effectverification.dto.request.*;
import com.bp20.backend.effectverification.dto.response.EffectVerificationResponse;
import com.bp20.backend.effectverification.dto.response.VerificationExecutionResponse;
import com.bp20.backend.effectverification.entity.EffectVerificationExecution;
import com.bp20.backend.effectverification.entity.VerificationStatus;
import com.bp20.backend.effectverification.repository.EffectVerificationExecutionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EffectVerificationLifecycleService {

    private static final int DEFAULT_PERIOD_DAYS = 14;
    private static final int MAX_PERIOD_DAYS = 90;

    private final EffectVerificationExecutionRepository executionRepository;
    private final EffectVerificationService verificationService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public VerificationExecutionResponse registerExecution(ExecutionRegistrationRequest request) {
        if (executionRepository.existsByAiRecommendationId(request.getRecommendationId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Verification execution already exists for this recommendation"
            );
        }

        validateMetrics(request.getRecommendationType(), request.getBefore(), "before");
        validateCondition(request.getRecommendationType(), request.getCondition());
        int periodDays = resolvePeriodDays(request.getCondition());
        LocalDateTime executedAt = request.getExecutedAt() != null
                ? request.getExecutedAt()
                : LocalDateTime.now();

        EffectVerificationExecution execution = EffectVerificationExecution.builder()
                .aiRecommendationId(request.getRecommendationId())
                .storeId(request.getStoreId())
                .recommendationType(request.getRecommendationType())
                .status(VerificationStatus.COLLECTING)
                .conditionJson(writeJson(request.getCondition()))
                .beforeMetricsJson(writeJson(request.getBefore()))
                .executedAt(executedAt)
                .verificationDueAt(executedAt.plusDays(periodDays))
                .build();

        return toResponse(executionRepository.save(execution));
    }

    public EffectVerificationResponse completeVerification(
            Long recommendationId,
            VerificationCompletionRequest request
    ) {
        EffectVerificationExecution execution = findExecution(recommendationId);
        if (execution.getStatus() == VerificationStatus.VERIFIED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Verification has already been completed"
            );
        }

        validateMetrics(execution.getRecommendationType(), request.getAfter(), "after");
        LocalDateTime collectedAt = request.getCollectedAt() != null
                ? request.getCollectedAt()
                : LocalDateTime.now();
        if (collectedAt.isBefore(execution.getVerificationDueAt())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Post-execution collection period has not ended"
            );
        }

        execution.markReady(writeJson(request.getAfter()));
        executionRepository.save(execution);

        EffectVerificationRequest verificationRequest = new EffectVerificationRequest();
        verificationRequest.setStoreId(execution.getStoreId());
        verificationRequest.setRecommendationId(execution.getAiRecommendationId());
        verificationRequest.setRecommendationType(execution.getRecommendationType());
        verificationRequest.setCondition(readJson(
                execution.getConditionJson(),
                VerificationCondition.class
        ));
        verificationRequest.setBefore(readJson(
                execution.getBeforeMetricsJson(),
                PeriodMetrics.class
        ));
        verificationRequest.setAfter(request.getAfter());

        try {
            EffectVerificationResponse response = verificationService.verifyEffect(verificationRequest);
            execution.markVerified(response.getVerifiedDate());
            executionRepository.save(execution);
            return response;
        } catch (RuntimeException exception) {
            execution.markFailed(exception.getMessage());
            executionRepository.save(execution);
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public VerificationExecutionResponse getExecution(Long recommendationId) {
        return toResponse(findExecution(recommendationId));
    }

    @Transactional(readOnly = true)
    public List<VerificationExecutionResponse> getDueExecutions(Long storeId) {
        LocalDateTime now = LocalDateTime.now();
        List<EffectVerificationExecution> executions = storeId == null
                ? executionRepository
                .findByStatusAndVerificationDueAtLessThanEqualOrderByVerificationDueAtAsc(
                        VerificationStatus.COLLECTING,
                        now
                )
                : executionRepository
                .findByStoreIdAndStatusAndVerificationDueAtLessThanEqualOrderByVerificationDueAtAsc(
                        storeId,
                        VerificationStatus.COLLECTING,
                        now
                );

        return executions.stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<VerificationExecutionResponse> getExecutionHistory(
            Long storeId,
            VerificationStatus status
    ) {
        List<EffectVerificationExecution> executions = status == null
                ? executionRepository.findByStoreIdOrderByExecutedAtDesc(storeId)
                : executionRepository.findByStoreIdAndStatusOrderByExecutedAtDesc(
                        storeId,
                        status
                );

        return executions.stream()
                .map(this::toResponse)
                .toList();
    }

    private EffectVerificationExecution findExecution(Long recommendationId) {
        return executionRepository.findByAiRecommendationId(recommendationId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Verification execution not found"
                ));
    }

    private int resolvePeriodDays(VerificationCondition condition) {
        Integer periodDays = condition.getPeriodDays();
        if (periodDays == null) {
            condition.setPeriodDays(DEFAULT_PERIOD_DAYS);
            return DEFAULT_PERIOD_DAYS;
        }
        if (periodDays < 1 || periodDays > MAX_PERIOD_DAYS) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "period_days must be between 1 and 90"
            );
        }
        return periodDays;
    }

    private void validateCondition(
            RecommendationType recommendationType,
            VerificationCondition condition
    ) {
        boolean hasStartHour = condition.getStartHour() != null;
        boolean hasEndHour = condition.getEndHour() != null;
        if (hasStartHour != hasEndHour) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "start_hour and end_hour must be provided together"
            );
        }
        if (hasStartHour && condition.getStartHour().equals(condition.getEndHour())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "start_hour and end_hour must be different"
            );
        }
        if (recommendationType == RecommendationType.REVIEW
                && !StringUtils.hasText(condition.getTargetAspect())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "REVIEW recommendation requires target_aspect"
            );
        }
    }

    private void validateMetrics(
            RecommendationType recommendationType,
            PeriodMetrics metrics,
            String fieldName
    ) {
        boolean valid = switch (recommendationType) {
            case SALES -> metrics.getSales() != null;
            case REVIEW -> metrics.getReview() != null;
        };
        if (!valid) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    fieldName + " metrics do not match recommendation_type"
            );
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize verification data", exception);
        }
    }

    private <T> T readJson(String value, Class<T> type) {
        try {
            return objectMapper.readValue(value, type);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to deserialize verification data", exception);
        }
    }

    private VerificationExecutionResponse toResponse(EffectVerificationExecution execution) {
        return VerificationExecutionResponse.builder()
                .storeId(execution.getStoreId())
                .recommendationId(execution.getAiRecommendationId())
                .recommendationType(execution.getRecommendationType())
                .status(execution.getStatus())
                .executedAt(execution.getExecutedAt())
                .verificationDueAt(execution.getVerificationDueAt())
                .verifiedAt(execution.getVerifiedAt())
                .failureReason(execution.getFailureReason())
                .build();
    }
}
