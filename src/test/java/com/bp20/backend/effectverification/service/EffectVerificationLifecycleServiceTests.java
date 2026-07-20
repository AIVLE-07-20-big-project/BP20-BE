package com.bp20.backend.effectverification.service;

import com.bp20.backend.effectverification.dto.request.*;
import com.bp20.backend.effectverification.dto.response.EffectVerificationResponse;
import com.bp20.backend.effectverification.dto.response.VerificationExecutionResponse;
import com.bp20.backend.effectverification.entity.EffectVerificationExecution;
import com.bp20.backend.effectverification.entity.VerificationStatus;
import com.bp20.backend.effectverification.repository.EffectVerificationExecutionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EffectVerificationLifecycleServiceTests {

    @Mock
    private EffectVerificationExecutionRepository executionRepository;

    @Mock
    private EffectVerificationService verificationService;

    private EffectVerificationLifecycleService lifecycleService;

    @BeforeEach
    void setUp() {
        lifecycleService = new EffectVerificationLifecycleService(
                executionRepository,
                verificationService
        );
    }

    @Test
    void registerExecutionStoresBaselineAndFourteenDayDueDate() {
        LocalDateTime executedAt = LocalDateTime.of(2026, 7, 20, 10, 0);
        ExecutionRegistrationRequest request = registrationRequest(executedAt);
        when(executionRepository.existsByAiRecommendationId(100L)).thenReturn(false);
        when(executionRepository.save(any(EffectVerificationExecution.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        VerificationExecutionResponse response = lifecycleService.registerExecution(request);

        assertThat(response.getStatus()).isEqualTo(VerificationStatus.COLLECTING);
        assertThat(response.getVerificationDueAt()).isEqualTo(executedAt.plusDays(14));
        ArgumentCaptor<EffectVerificationExecution> captor =
                ArgumentCaptor.forClass(EffectVerificationExecution.class);
        verify(executionRepository).save(captor.capture());
        assertThat(captor.getValue().getBeforeMetricsJson()).contains("target_sales");
    }

    @Test
    void completeVerificationRejectsCollectionBeforeDueDate() {
        LocalDateTime executedAt = LocalDateTime.of(2026, 7, 20, 10, 0);
        EffectVerificationExecution execution = savedExecution(executedAt);
        when(executionRepository.findByAiRecommendationId(100L))
                .thenReturn(Optional.of(execution));
        VerificationCompletionRequest request = new VerificationCompletionRequest();
        request.setAfter(salesPeriod(1_300_000.0));
        request.setCollectedAt(executedAt.plusDays(13));

        assertThatThrownBy(() -> lifecycleService.completeVerification(100L, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("collection period has not ended");
    }

    @Test
    void completeVerificationBuildsAiRequestFromStoredBaseline() {
        LocalDateTime executedAt = LocalDateTime.of(2026, 7, 1, 10, 0);
        EffectVerificationExecution execution = savedExecution(executedAt);
        when(executionRepository.findByAiRecommendationId(100L))
                .thenReturn(Optional.of(execution));
        when(executionRepository.save(any(EffectVerificationExecution.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LocalDateTime verifiedAt = LocalDateTime.of(2026, 7, 20, 11, 0);
        EffectVerificationResponse aiResponse = new EffectVerificationResponse();
        aiResponse.setVerifiedDate(verifiedAt);
        when(verificationService.verifyEffect(any(EffectVerificationRequest.class)))
                .thenReturn(aiResponse);

        VerificationCompletionRequest request = new VerificationCompletionRequest();
        request.setAfter(salesPeriod(1_300_000.0));
        request.setCollectedAt(executedAt.plusDays(14));

        lifecycleService.completeVerification(100L, request);

        ArgumentCaptor<EffectVerificationRequest> captor =
                ArgumentCaptor.forClass(EffectVerificationRequest.class);
        verify(verificationService).verifyEffect(captor.capture());
        EffectVerificationRequest sent = captor.getValue();
        assertThat(sent.getRecommendationId()).isEqualTo(100L);
        assertThat(sent.getBefore().getSales().getTargetSales()).isEqualTo(1_000_000.0);
        assertThat(sent.getAfter().getSales().getTargetSales()).isEqualTo(1_300_000.0);
        assertThat(execution.getStatus()).isEqualTo(VerificationStatus.VERIFIED);
        assertThat(execution.getVerifiedAt()).isEqualTo(verifiedAt);
    }

    @Test
    void getDueExecutionsReturnsCollectingExecutionsForStore() {
        LocalDateTime executedAt = LocalDateTime.now().minusDays(15);
        EffectVerificationExecution execution = savedExecution(executedAt);
        when(executionRepository
                .findByStoreIdAndStatusAndVerificationDueAtLessThanEqualOrderByVerificationDueAtAsc(
                        any(Long.class),
                        any(VerificationStatus.class),
                        any(LocalDateTime.class)
                ))
                .thenReturn(List.of(execution));

        List<VerificationExecutionResponse> responses =
                lifecycleService.getDueExecutions(1L);

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().getRecommendationId()).isEqualTo(100L);
        assertThat(responses.getFirst().getStatus()).isEqualTo(VerificationStatus.COLLECTING);
        verify(executionRepository)
                .findByStoreIdAndStatusAndVerificationDueAtLessThanEqualOrderByVerificationDueAtAsc(
                        any(Long.class),
                        any(VerificationStatus.class),
                        any(LocalDateTime.class)
                );
    }

    private ExecutionRegistrationRequest registrationRequest(LocalDateTime executedAt) {
        ExecutionRegistrationRequest request = new ExecutionRegistrationRequest();
        request.setStoreId(1L);
        request.setRecommendationId(100L);
        request.setRecommendationType(RecommendationType.SALES);
        request.setCondition(new VerificationCondition(14, 14, 17, true, null));
        request.setBefore(salesPeriod(1_000_000.0));
        request.setExecutedAt(executedAt);
        return request;
    }

    private EffectVerificationExecution savedExecution(LocalDateTime executedAt) {
        return EffectVerificationExecution.builder()
                .aiRecommendationId(100L)
                .storeId(1L)
                .recommendationType(RecommendationType.SALES)
                .status(VerificationStatus.COLLECTING)
                .conditionJson("{\"period_days\":14,\"start_hour\":14,\"end_hour\":17,\"compare_same_weekday\":true}")
                .beforeMetricsJson("{\"sales\":{\"target_sales\":1000000.0,\"visit_count\":100,\"average_order_value\":10000.0,\"revisit_rate\":20.0,\"coupon_usage_rate\":10.0,\"new_customer_count\":20,\"dormant_customer_return_count\":5,\"total_sales\":5000000.0},\"review\":null}")
                .executedAt(executedAt)
                .verificationDueAt(executedAt.plusDays(14))
                .build();
    }

    private PeriodMetrics salesPeriod(double targetSales) {
        SalesMetrics sales = new SalesMetrics(
                targetSales,
                100,
                10_000.0,
                20.0,
                10.0,
                20,
                5,
                5_000_000.0
        );
        return new PeriodMetrics(sales, null);
    }
}
