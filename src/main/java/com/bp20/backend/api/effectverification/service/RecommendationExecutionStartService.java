package com.bp20.backend.api.effectverification.service;

import com.bp20.backend.api.effectverification.client.CampaignExecutionClient;
import com.bp20.backend.api.effectverification.collector.VerificationMetricCollector;
import com.bp20.backend.api.effectverification.dto.request.ExecutionRegistrationRequest;
import com.bp20.backend.api.effectverification.dto.request.PeriodMetrics;
import com.bp20.backend.api.effectverification.dto.request.RecommendationExecutionStartRequest;
import com.bp20.backend.api.effectverification.dto.request.RecommendationType;
import com.bp20.backend.api.effectverification.dto.request.SelectedActionRequest;
import com.bp20.backend.api.effectverification.dto.request.VerificationCondition;
import com.bp20.backend.api.effectverification.dto.response.VerificationExecutionResponse;
import com.bp20.backend.api.effectverification.repository.EffectVerificationExecutionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RecommendationExecutionStartService {

    private static final int DEFAULT_PERIOD_DAYS = 30;

    private final ObjectProvider<VerificationMetricCollector> metricCollectorProvider;
    private final CampaignExecutionClient campaignExecutionClient;
    private final EffectVerificationExecutionRepository executionRepository;
    private final EffectVerificationLifecycleService lifecycleService;
    private final EffectVerificationStoreAccessService storeAccessService;
    private final Clock clock;

    public VerificationExecutionResponse start(
            Long userId,
            RecommendationExecutionStartRequest input
    ) {
        if (executionRepository.existsByRecommendationRun_ThreadId(input.getThreadId())) {
            return lifecycleService.getExecution(userId, input.getThreadId());
        }

        VerificationMetricCollector metricCollector =
                metricCollectorProvider.getIfAvailable();
        if (metricCollector == null) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Verification metric collector is not configured"
            );
        }

        RecommendationType recommendationType =
                input.getRecommendationType() == null
                        ? RecommendationType.SALES
                        : input.getRecommendationType();
        if (recommendationType == RecommendationType.REVIEW
                && !StringUtils.hasText(input.getTargetAspect())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "REVIEW recommendation requires target_aspect"
            );
        }
        // 사용자가 추천 승인 화면에서 고른 적용 기간을 그대로 쓴다 — 없으면 지금부터 14일 기본값.
        LocalDateTime executedAt = input.getExecutionStartedAt() != null
                ? input.getExecutionStartedAt()
                : LocalDateTime.now(clock);
        int periodDays = (input.getExecutionStartedAt() != null && input.getExecutionEndedAt() != null)
                ? (int) Math.max(
                        1,
                        java.time.Duration.between(
                                input.getExecutionStartedAt(), input.getExecutionEndedAt()
                        ).toDays()
                )
                : DEFAULT_PERIOD_DAYS;

        Map<String, Object> campaign = campaignExecutionClient.recordExecution(
                input.getThreadId(),
                userId,
                treatmentYyquCd(executedAt),
                executedAt
        );
        Long storeId = requiredLong(campaign, "store_id");
        storeAccessService.validateOwner(userId, storeId);
        String decisionId = optionalString(campaign, "decision_id");
        String actionId = firstRequiredString(
                campaign,
                "action_id",
                "executed_action",
                "approved_action"
        );
        // AI campaign-logs 응답은 "action_id"가 아니라 "executed_action"(executed=true로 보냈을 때
        // 채워지는 필드)을 쓴다 — approved_action은 승인만 됐을 뿐 실행 여부와 무관해 값이 다를 수 있다.

        VerificationCondition condition = new VerificationCondition(
                periodDays,
                null,
                null,
                false,
                input.getTargetAspect()
        );

        PeriodMetrics before = metricCollector.collect(
                storeId,
                recommendationType,
                executedAt.minusDays(periodDays),
                executedAt,
                condition
        );

        ExecutionRegistrationRequest request = new ExecutionRegistrationRequest();
        request.setThreadId(input.getThreadId());
        request.setDecisionId(decisionId);
        request.setStoreId(storeId);
        request.setRecommendationType(recommendationType);
        request.setCondition(condition);
        request.setBefore(before);
        SelectedActionRequest selectedAction = new SelectedActionRequest();
        selectedAction.setAction(actionId);
        request.setSelectedAction(selectedAction);
        request.setExecutedAt(executedAt);

        return lifecycleService.registerExecution(userId, request);
    }

    private int treatmentYyquCd(LocalDateTime executedAt) {
        int quarter = (executedAt.getMonthValue() - 1) / 3 + 1;
        return executedAt.getYear() * 10 + quarter;
    }

    private String requiredString(Map<String, Object> value, String key) {
        Object field = value.get(key);
        if (field == null || field.toString().isBlank()) {
            throw invalidCampaignResponse(key);
        }
        return field.toString();
    }

    private String optionalString(Map<String, Object> value, String key) {
        Object field = value.get(key);
        return field == null || field.toString().isBlank()
                ? null
                : field.toString();
    }

    private String firstRequiredString(
            Map<String, Object> value,
            String... keys
    ) {
        for (String key : keys) {
            Object field = value.get(key);
            if (field != null && !field.toString().isBlank()) {
                return field.toString();
            }
        }
        throw invalidCampaignResponse(String.join(" or ", keys));
    }

    private Long requiredLong(Map<String, Object> value, String key) {
        Object field = value.get(key);
        if (field instanceof Number number) {
            return number.longValue();
        }
        if (field == null || field.toString().isBlank()) {
            throw invalidCampaignResponse(key);
        }
        try {
            return Long.valueOf(field.toString());
        } catch (NumberFormatException exception) {
            throw invalidCampaignResponse(key);
        }
    }

    private ResponseStatusException invalidCampaignResponse(String key) {
        return new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "Campaign execution response is missing a valid " + key
        );
    }
}
