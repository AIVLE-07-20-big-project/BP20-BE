package com.bp20.backend.api.effectverification.service;

import com.bp20.backend.api.effectverification.collector.VerificationMetricCollector;
import com.bp20.backend.api.effectverification.dto.request.ExecutionRegistrationRequest;
import com.bp20.backend.api.effectverification.dto.request.PeriodMetrics;
import com.bp20.backend.api.effectverification.dto.request.RecommendationType;
import com.bp20.backend.api.effectverification.dto.request.VerificationCondition;
import com.bp20.backend.api.effectverification.dto.response.VerificationExecutionResponse;
import com.bp20.backend.api.effectverification.repository.EffectVerificationExecutionRepository;
import com.bp20.backend.api.store.service.StoreReviewRecommendationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewRecommendationExecutionStartServiceTests {

    @Mock
    private ObjectProvider<VerificationMetricCollector> collectorProvider;
    @Mock
    private VerificationMetricCollector collector;
    @Mock
    private EffectVerificationExecutionRepository executionRepository;
    @Mock
    private EffectVerificationLifecycleService lifecycleService;
    @Mock
    private EffectVerificationStoreAccessService storeAccessService;
    @Mock
    private StoreReviewRecommendationService recommendationService;

    @Test
    void registersReviewVerificationWhenActionIsCompleted() {
        ReviewRecommendationExecutionStartService service = service();
        LocalDateTime executedAt = LocalDateTime.of(2026, 8, 3, 14, 0);
        StoreReviewRecommendationService.CompletedAction action =
                new StoreReviewRecommendationService.CompletedAction(
                        "review-7-1",
                        1L,
                        "food",
                        "레시피를 정량화합니다.",
                        executedAt,
                        true
                );
        PeriodMetrics before = new PeriodMetrics();
        VerificationExecutionResponse expected =
                VerificationExecutionResponse.builder().build();
        when(recommendationService.completeActionItem(10L, 7L, "음식 간이 맞지 않음"))
                .thenReturn(action);
        when(executionRepository.existsByAiRecommendationId("review-7-1"))
                .thenReturn(false);
        when(collectorProvider.getIfAvailable()).thenReturn(collector);
        when(storeAccessService.resolveOwnedStoreId(10L, 1L)).thenReturn(1L);
        when(collector.collect(
                eq(1L),
                eq(RecommendationType.REVIEW),
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                any(VerificationCondition.class)
        )).thenReturn(before);
        when(lifecycleService.registerExecution(
                eq(10L),
                any(ExecutionRegistrationRequest.class)
        )).thenReturn(expected);

        VerificationExecutionResponse actual = service.startAction(
                10L,
                7L,
                "음식 간이 맞지 않음"
        );

        ArgumentCaptor<ExecutionRegistrationRequest> captor =
                ArgumentCaptor.forClass(ExecutionRegistrationRequest.class);
        verify(lifecycleService).registerExecution(eq(10L), captor.capture());
        ExecutionRegistrationRequest request = captor.getValue();
        assertThat(actual).isSameAs(expected);
        assertThat(request.getRecommendationId()).isEqualTo("review-7-1");
        assertThat(request.getStoreId()).isEqualTo(1L);
        assertThat(request.getRecommendationType()).isEqualTo(RecommendationType.REVIEW);
        assertThat(request.getCondition().getTargetAspect()).isEqualTo("food");
        assertThat(request.getCondition().getPeriodDays()).isEqualTo(30);
        assertThat(request.getBefore()).isSameAs(before);
        assertThat(request.getSelectedAction().getAction())
                .isEqualTo("레시피를 정량화합니다.");
        assertThat(request.getExecutedAt()).isEqualTo(executedAt);
        verify(collector).collect(
                1L,
                RecommendationType.REVIEW,
                executedAt.minusDays(30),
                executedAt,
                request.getCondition()
        );
    }

    @Test
    void reusesExistingVerificationForRepeatedCompletion() {
        ReviewRecommendationExecutionStartService service = service();
        StoreReviewRecommendationService.CompletedAction action =
                new StoreReviewRecommendationService.CompletedAction(
                        "review-7-1",
                        1L,
                        "food",
                        "action",
                        LocalDateTime.now(),
                        false
                );
        VerificationExecutionResponse expected =
                VerificationExecutionResponse.builder().build();
        when(recommendationService.completeActionItem(10L, 7L, "keyword"))
                .thenReturn(action);
        when(executionRepository.existsByAiRecommendationId("review-7-1"))
                .thenReturn(true);
        when(storeAccessService.resolveOwnedStoreId(10L, 1L)).thenReturn(1L);
        when(lifecycleService.getExecution(10L, "review-7-1"))
                .thenReturn(expected);

        VerificationExecutionResponse actual = service.startAction(
                10L,
                7L,
                "keyword"
        );

        assertThat(actual).isSameAs(expected);
        verify(collectorProvider, never()).getIfAvailable();
        verify(lifecycleService, never()).registerExecution(any(), any());
    }

    private ReviewRecommendationExecutionStartService service() {
        return new ReviewRecommendationExecutionStartService(
                collectorProvider,
                executionRepository,
                lifecycleService,
                storeAccessService,
                recommendationService
        );
    }
}
