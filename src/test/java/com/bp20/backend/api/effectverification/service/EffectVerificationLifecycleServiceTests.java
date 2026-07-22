package com.bp20.backend.api.effectverification.service;

import com.bp20.backend.api.effectverification.dto.request.*;
import com.bp20.backend.api.effectverification.dto.response.EffectVerificationResponse;
import com.bp20.backend.api.effectverification.dto.response.VerificationExecutionResponse;
import com.bp20.backend.api.effectverification.domain.EffectVerificationExecution;
import com.bp20.backend.api.effectverification.domain.VerificationStatus;
import com.bp20.backend.api.effectverification.repository.EffectVerificationExecutionRepository;
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
import static org.mockito.Mockito.never;
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

    @Test
    void getExecutionHistoryFiltersByStoreAndStatus() {
        EffectVerificationExecution execution = savedExecution(
                LocalDateTime.of(2026, 7, 1, 10, 0)
        );
        when(executionRepository.findByStoreIdAndStatusOrderByExecutedAtDesc(
                1L,
                VerificationStatus.COLLECTING
        )).thenReturn(List.of(execution));

        List<VerificationExecutionResponse> responses =
                lifecycleService.getExecutionHistory(1L, VerificationStatus.COLLECTING);

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().getStoreId()).isEqualTo(1L);
        assertThat(responses.getFirst().getStatus()).isEqualTo(VerificationStatus.COLLECTING);
        verify(executionRepository).findByStoreIdAndStatusOrderByExecutedAtDesc(
                1L,
                VerificationStatus.COLLECTING
        );
        verify(executionRepository, never()).findByStoreIdOrderByExecutedAtDesc(1L);
    }

    @Test
    void reviewExecutionCompletesWithStoredReviewBaseline() {
        LocalDateTime executedAt = LocalDateTime.of(2026, 7, 1, 10, 0);
        ExecutionRegistrationRequest registration = new ExecutionRegistrationRequest();
        registration.setStoreId(1L);
        registration.setRecommendationId(200L);
        registration.setRecommendationType(RecommendationType.REVIEW);
        registration.setCondition(new VerificationCondition(
                14,
                null,
                null,
                true,
                "waiting_time"
        ));
        registration.setBefore(reviewPeriod(3.8, 40.0));
        registration.setExecutedAt(executedAt);

        when(executionRepository.existsByAiRecommendationId(200L)).thenReturn(false);
        when(executionRepository.save(any(EffectVerificationExecution.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        lifecycleService.registerExecution(registration);

        ArgumentCaptor<EffectVerificationExecution> executionCaptor =
                ArgumentCaptor.forClass(EffectVerificationExecution.class);
        verify(executionRepository).save(executionCaptor.capture());
        EffectVerificationExecution saved = executionCaptor.getValue();
        when(executionRepository.findByAiRecommendationId(200L))
                .thenReturn(Optional.of(saved));

        EffectVerificationResponse aiResponse = new EffectVerificationResponse();
        aiResponse.setVerifiedDate(executedAt.plusDays(15));
        when(verificationService.verifyEffect(any(EffectVerificationRequest.class)))
                .thenReturn(aiResponse);

        VerificationCompletionRequest completion = new VerificationCompletionRequest();
        completion.setAfter(reviewPeriod(4.4, 18.0));
        completion.setCollectedAt(executedAt.plusDays(15));

        lifecycleService.completeVerification(200L, completion);

        ArgumentCaptor<EffectVerificationRequest> requestCaptor =
                ArgumentCaptor.forClass(EffectVerificationRequest.class);
        verify(verificationService).verifyEffect(requestCaptor.capture());
        EffectVerificationRequest sent = requestCaptor.getValue();
        assertThat(sent.getRecommendationType()).isEqualTo(RecommendationType.REVIEW);
        assertThat(sent.getCondition().getTargetAspect()).isEqualTo("waiting_time");
        assertThat(sent.getBefore().getReview().getAverageRating()).isEqualTo(3.8);
        assertThat(sent.getAfter().getReview().getAverageRating()).isEqualTo(4.4);
        assertThat(saved.getStatus()).isEqualTo(VerificationStatus.VERIFIED);
    }

    @Test
    void reviewRegistrationRejectsMissingTargetAspect() {
        ExecutionRegistrationRequest request = new ExecutionRegistrationRequest();
        request.setStoreId(1L);
        request.setRecommendationId(201L);
        request.setRecommendationType(RecommendationType.REVIEW);
        request.setCondition(new VerificationCondition(14, null, null, true, null));
        request.setBefore(reviewPeriod(3.8, 40.0));

        assertThatThrownBy(() -> lifecycleService.registerExecution(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("requires target_aspect");
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

    private PeriodMetrics reviewPeriod(double averageRating, double negativeRate) {
        ReviewMetrics review = new ReviewMetrics(
                averageRating,
                negativeRate,
                20,
                negativeRate,
                0.9,
                50,
                25.0,
                5_000_000.0
        );
        return new PeriodMetrics(null, review);
    }
}
