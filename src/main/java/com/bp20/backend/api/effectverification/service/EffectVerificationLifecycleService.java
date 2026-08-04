package com.bp20.backend.api.effectverification.service;

import com.bp20.backend.api.ai.domain.AiRecommendationRun;
import com.bp20.backend.api.ai.repository.AiRecommendationRunRepository;
import com.bp20.backend.api.effectverification.dto.request.*;
import com.bp20.backend.api.effectverification.dto.response.EffectVerificationResponse;
import com.bp20.backend.api.effectverification.dto.response.VerificationExecutionResponse;
import com.bp20.backend.api.effectverification.domain.EffectVerificationExecution;
import com.bp20.backend.api.effectverification.domain.VerificationStatus;
import com.bp20.backend.api.effectverification.repository.EffectVerificationExecutionRepository;
import com.bp20.backend.api.store.domain.Store;
import com.bp20.backend.api.store.repository.StoreRepository;
import com.bp20.backend.api.user.domain.User;
import com.bp20.backend.api.user.repository.UserRepository;
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
    private final AiRecommendationRunRepository recommendationRunRepository;
    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public VerificationExecutionResponse registerExecution(
            Long userId,
            ExecutionRegistrationRequest request
    ) {
        String recommendationId = resolveRecommendationId(request);
        if (executionRepository.existsByAiRecommendationId(recommendationId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Verification execution already exists for this recommendation"
            );
        }
        if (StringUtils.hasText(request.getThreadId())
                && executionRepository.existsByRecommendationRun_ThreadId(
                        request.getThreadId()
                )) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Verification execution already exists for this thread"
            );
        }
        if (StringUtils.hasText(request.getDecisionId())
                && executionRepository.existsByDecisionId(request.getDecisionId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Verification execution already exists for this campaign decision"
            );
        }

        validateMetrics(request.getRecommendationType(), request.getBefore(), "before");
        validateCondition(request.getRecommendationType(), request.getCondition());
        int periodDays = resolvePeriodDays(request.getCondition());
        LocalDateTime executedAt = request.getExecutedAt() != null
                ? request.getExecutedAt()
                : LocalDateTime.now();
        User user = findOptionalUser(userId);
        Store store = findStore(request.getStoreId());
        AiRecommendationRun recommendationRun =
                findRecommendationRun(userId, request.getThreadId());

        EffectVerificationExecution execution = EffectVerificationExecution.builder()
                .aiRecommendationId(recommendationId)
                .recommendationRun(recommendationRun)
                .decisionId(request.getDecisionId())
                .user(user)
                .store(store)
                .recommendationType(request.getRecommendationType())
                .status(VerificationStatus.COLLECTING)
                .conditionJson(writeJson(request.getCondition()))
                .beforeMetricsJson(writeJson(request.getBefore()))
                .selectedActionJson(request.getSelectedAction() == null
                        ? null
                        : writeJson(request.getSelectedAction()))
                .executedAt(executedAt)
                .verificationDueAt(executedAt.plusDays(periodDays))
                .build();

        return toResponse(executionRepository.save(execution));
    }

    private String resolveRecommendationId(ExecutionRegistrationRequest request) {
        if (StringUtils.hasText(request.getRecommendationId())) {
            return request.getRecommendationId();
        }
        if (StringUtils.hasText(request.getThreadId())) {
            return request.getThreadId();
        }
        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "recommendation_id or thread_id is required"
        );
    }

    @Transactional
    public EffectVerificationResponse completeVerification(
            Long userId,
            String recommendationId,
            VerificationCompletionRequest request
    ) {
        EffectVerificationExecution execution = findExecution(userId, recommendationId);
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
        execution.beginAttempt(LocalDateTime.now());
        executionRepository.save(execution);

        EffectVerificationRequest verificationRequest = new EffectVerificationRequest();
        verificationRequest.setStoreId(execution.getStore().getId());
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
            EffectVerificationResponse response = verificationService.verifyEffect(
                    execution.getUser() == null ? null : execution.getUser().getId(),
                    verificationRequest
            );
            execution.markVerified(response.getVerifiedDate());
            executionRepository.save(execution);
            return response;
        } catch (RuntimeException exception) {
            execution.markFailed(exception.getMessage());
            executionRepository.save(execution);
            throw exception;
        }
    }

    /**
     * 승인된 추천(thread)의 적용전 분석(analysis_id)과, 사용자가 고른 적용후 분석을
     * AI의 verify-from-analyses에 그대로 넘겨 8개 매출 지표 전체를 실제값으로 검증한다.
     * 스케줄러의 14일 대기와 무관하게 즉시 완료할 수 있다.
     */
    @Transactional
    public EffectVerificationResponse completeVerificationFromAnalyses(
            Long userId, String threadId, String afterAnalysisId
    ) {
        EffectVerificationExecution execution = findExecution(userId, threadId);
        if (execution.getStatus() == VerificationStatus.VERIFIED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Verification has already been completed"
            );
        }
        AiRecommendationRun run = recommendationRunRepository
                .findByThreadIdAndUser_Id(threadId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "AI recommendation run not found for thread"
                ));
        VerificationCondition condition = readJson(
                execution.getConditionJson(), VerificationCondition.class
        );

        EffectVerificationResponse response = verificationService.verifyEffectFromAnalyses(
                userId,
                run.getAnalysis().getAnalysisId(),
                afterAnalysisId,
                execution.getStore().getId(),
                execution.getEffectVerificationExecutionId(),
                execution.getAiRecommendationId(),
                condition.getStartHour(),
                condition.getEndHour()
        );

        execution.beginAttempt(LocalDateTime.now());
        execution.markVerified(response.getVerifiedDate());
        executionRepository.save(execution);
        return response;
    }

    /** AI 매출 CSV 실측값을 매출형 전략 검증 결과로 확정한다. */
    @Transactional
    public EffectVerificationResponse completeSalesVerificationFromCsv(
            Long userId, String threadId, double afterSales, LocalDateTime collectedAt
    ) {
        EffectVerificationExecution execution = executionRepository
                .findByRecommendationRun_ThreadIdAndUser_Id(threadId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Verification execution not found for thread"
                ));
        PeriodMetrics before = readJson(execution.getBeforeMetricsJson(), PeriodMetrics.class);
        SalesMetrics beforeSales = before.getSales();
        if (beforeSales == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sales before metrics are missing");
        }
        SalesMetrics after = new SalesMetrics(
                beforeSales.getTargetSales(), beforeSales.getVisitCount(),
                beforeSales.getAverageOrderValue(), beforeSales.getRevisitRate(),
                beforeSales.getCouponUsageRate(), beforeSales.getNewCustomerCount(),
                beforeSales.getDormantCustomerReturnCount(), afterSales
        );
        VerificationCompletionRequest request = new VerificationCompletionRequest();
        request.setAfter(new PeriodMetrics(after, null));
        request.setCollectedAt(collectedAt);
        return completeVerification(userId, execution.getAiRecommendationId(), request);
    }

    @Transactional(readOnly = true)
    public VerificationExecutionResponse getExecution(
            Long userId,
            String recommendationId
    ) {
        return toResponse(findExecution(userId, recommendationId));
    }

    @Transactional(readOnly = true)
    public AutomaticCollectionContext getAutomaticCollectionContext(
            String recommendationId
    ) {
        EffectVerificationExecution execution = findExecution(null, recommendationId);
        return new AutomaticCollectionContext(
                execution.getStore().getId(),
                execution.getRecommendationType(),
                readJson(execution.getConditionJson(), VerificationCondition.class),
                execution.getExecutedAt(),
                execution.getVerificationDueAt()
        );
    }

    @Transactional
    public VerificationExecutionResponse linkCampaignDecision(
            Long userId,
            String threadId,
            String decisionId
    ) {
        EffectVerificationExecution execution = (userId == null
                ? executionRepository.findByRecommendationRun_ThreadId(threadId)
                : executionRepository.findByRecommendationRun_ThreadIdAndUser_Id(
                        threadId,
                        userId
                ))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Verification execution not found for thread"
                ));

        if (decisionId.equals(execution.getDecisionId())) {
            return toResponse(execution);
        }
        if (execution.getDecisionId() != null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Campaign decision is already linked to this verification"
            );
        }
        if (executionRepository.existsByDecisionId(decisionId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Campaign decision is already linked to another verification"
            );
        }

        execution.linkDecision(decisionId);
        return toResponse(executionRepository.save(execution));
    }

    @Transactional(readOnly = true)
    public List<VerificationExecutionResponse> getDueExecutions(
            Long userId,
            Long storeId
    ) {
        LocalDateTime now = LocalDateTime.now();
        List<EffectVerificationExecution> executions;
        if (userId == null) {
            executions = storeId == null
                    ? executionRepository
                    .findByStatusAndVerificationDueAtLessThanEqualOrderByVerificationDueAtAsc(
                            VerificationStatus.COLLECTING,
                            now
                    )
                    : executionRepository
                    .findByStore_IdAndStatusAndVerificationDueAtLessThanEqualOrderByVerificationDueAtAsc(
                            storeId,
                            VerificationStatus.COLLECTING,
                            now
                    );
        } else {
            executions = storeId == null
                    ? executionRepository
                    .findByUser_IdAndStatusAndVerificationDueAtLessThanEqualOrderByVerificationDueAtAsc(
                            userId,
                            VerificationStatus.COLLECTING,
                            now
                    )
                    : executionRepository
                    .findByUser_IdAndStore_IdAndStatusAndVerificationDueAtLessThanEqualOrderByVerificationDueAtAsc(
                            userId,
                            storeId,
                            VerificationStatus.COLLECTING,
                            now
                    );
        }

        return executions.stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<VerificationExecutionResponse> getExecutionHistory(
            Long userId,
            Long storeId,
            VerificationStatus status
    ) {
        List<EffectVerificationExecution> executions;
        if (userId == null) {
            executions = status == null
                    ? executionRepository.findByStore_IdOrderByExecutedAtDesc(storeId)
                    : executionRepository.findByStore_IdAndStatusOrderByExecutedAtDesc(
                            storeId,
                            status
                    );
        } else {
            executions = status == null
                    ? executionRepository.findByUser_IdAndStore_IdOrderByExecutedAtDesc(
                            userId,
                            storeId
                    )
                    : executionRepository.findByUser_IdAndStore_IdAndStatusOrderByExecutedAtDesc(
                            userId,
                            storeId,
                            status
                    );
        }

        return executions.stream()
                .map(this::toResponse)
                .toList();
    }

    private EffectVerificationExecution findExecution(
            Long userId,
            String recommendationId
    ) {
        return (userId == null
                ? executionRepository.findByAiRecommendationId(recommendationId)
                : executionRepository.findByAiRecommendationIdAndUser_Id(
                        recommendationId,
                        userId
                ))
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
                .storeId(execution.getStore().getId())
                .recommendationId(execution.getAiRecommendationId())
                .threadId(execution.getRecommendationRun() == null
                        ? null
                        : execution.getRecommendationRun().getThreadId())
                .decisionId(execution.getDecisionId())
                .recommendationType(execution.getRecommendationType())
                .selectedAction(execution.getSelectedActionJson() == null
                        ? null
                        : readJson(
                                execution.getSelectedActionJson(),
                                SelectedActionRequest.class
                        ))
                .status(execution.getStatus())
                .executedAt(execution.getExecutedAt())
                .verificationDueAt(execution.getVerificationDueAt())
                .verifiedAt(execution.getVerifiedAt())
                .failureReason(execution.getFailureReason())
                .attemptCount(execution.getAttemptCount())
                .lastAttemptAt(execution.getLastAttemptAt())
                .build();
    }

    private User findOptionalUser(Long userId) {
        if (userId == null) {
            return null;
        }
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));
    }

    private Store findStore(Long storeId) {
        return storeRepository.findById(storeId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Store not found"
                ));
    }

    private AiRecommendationRun findRecommendationRun(
            Long userId,
            String threadId
    ) {
        if (!StringUtils.hasText(threadId)) {
            return null;
        }
        return (userId == null
                ? recommendationRunRepository.findById(threadId)
                : recommendationRunRepository.findByThreadIdAndUser_Id(threadId, userId))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "AI recommendation run not found"
                ));
    }

    public record AutomaticCollectionContext(
            Long storeId,
            RecommendationType recommendationType,
            VerificationCondition condition,
            LocalDateTime collectionFrom,
            LocalDateTime collectionTo
    ) {
    }
}
