package com.bp20.backend.api.effectverification.service;

import com.bp20.backend.api.effectverification.collector.VerificationMetricCollector;
import com.bp20.backend.api.effectverification.dto.request.ExecutionRegistrationRequest;
import com.bp20.backend.api.effectverification.dto.request.PeriodMetrics;
import com.bp20.backend.api.effectverification.dto.request.MockThreadExecutionRegistrationRequest;
import com.bp20.backend.api.effectverification.dto.request.RecommendationType;
import com.bp20.backend.api.effectverification.dto.request.SalesMetrics;
import com.bp20.backend.api.effectverification.dto.request.VerificationCompletionRequest;
import com.bp20.backend.api.effectverification.dto.request.VerificationCondition;
import com.bp20.backend.api.effectverification.dto.response.EffectVerificationResponse;
import com.bp20.backend.api.effectverification.dto.response.VerificationExecutionResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class MockAutomaticExecutionServiceTests {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private VerificationMetricCollector metricCollector;

    @Mock
    private MockReviewSentimentService reviewSentimentService;

    @Mock
    private EffectVerificationLifecycleService lifecycleService;

    @Mock
    private MockRecommendationFeedbackService feedbackService;

    @Mock
    private EffectVerificationStoreAccessService storeAccessService;

    private MockAutomaticExecutionService service;

    @BeforeEach
    void setUp() {
        service = new MockAutomaticExecutionService(
                jdbcTemplate,
                metricCollector,
                reviewSentimentService,
                lifecycleService,
                feedbackService,
                storeAccessService
        );
        lenient().when(storeAccessService.resolveOwnedStoreId(any(), anyLong()))
                .thenAnswer(invocation -> invocation.getArgument(1));
    }

    @Test
    void salesRecommendationCollectsFourteenDayBaselineAndRegistersExecution() {
        LocalDateTime executedAt = LocalDateTime.of(2026, 6, 15, 10, 0);
        mockRecommendation(new MockAutomaticExecutionService.MockRecommendation(
                10001L,
                1L,
                RecommendationType.SALES,
                14,
                17,
                null,
                true,
                executedAt
        ));
        PeriodMetrics baseline = new PeriodMetrics(
                new SalesMetrics(30_000.0, 3, 10_000.0, 33.33,
                        0.0, 1, 2, 50_000.0),
                null
        );
        when(metricCollector.collect(anyLong(), any(), any(), any(), any()))
                .thenReturn(baseline);
        when(lifecycleService.registerExecution(isNull(), any()))
                .thenReturn(VerificationExecutionResponse.builder().build());

        service.registerAutomatically(10001L);

        ArgumentCaptor<ExecutionRegistrationRequest> captor =
                ArgumentCaptor.forClass(ExecutionRegistrationRequest.class);
        verify(lifecycleService).registerExecution(isNull(), captor.capture());
        ExecutionRegistrationRequest request = captor.getValue();
        assertThat(request.getRecommendationId()).isEqualTo("10001");
        assertThat(request.getExecutedAt()).isEqualTo(executedAt);
        assertThat(request.getBefore()).isSameAs(baseline);
        assertThat(request.getCondition().getPeriodDays()).isEqualTo(14);
        verify(metricCollector).collect(
                eq(1L),
                eq(RecommendationType.SALES),
                eq(executedAt.minusDays(14)),
                eq(executedAt),
                any()
        );
        verify(reviewSentimentService, never())
                .analyzePending(anyLong(), any(), any());
    }

    @Test
    void threadRecommendationCollectsBaselineAndUsesThreadAsRecommendationId() {
        String threadId = "11111111-1111-1111-1111-111111111111";
        LocalDateTime executedAt = LocalDateTime.of(2026, 6, 15, 10, 0);
        VerificationCondition condition = new VerificationCondition(
                14, 14, 17, true, null
        );
        MockThreadExecutionRegistrationRequest input =
                new MockThreadExecutionRegistrationRequest();
        input.setStoreId(1L);
        input.setRecommendationType(RecommendationType.SALES);
        input.setCondition(condition);
        input.setExecutedAt(executedAt);
        PeriodMetrics baseline = new PeriodMetrics(
                new SalesMetrics(30_000.0, 3, 10_000.0, 33.33,
                        0.0, 1, 2, 50_000.0),
                null
        );
        when(metricCollector.collect(
                1L,
                RecommendationType.SALES,
                executedAt.minusDays(14),
                executedAt,
                condition
        )).thenReturn(baseline);
        when(lifecycleService.registerExecution(isNull(), any()))
                .thenReturn(VerificationExecutionResponse.builder().build());

        service.registerThreadAutomatically(threadId, input);

        ArgumentCaptor<ExecutionRegistrationRequest> captor =
                ArgumentCaptor.forClass(ExecutionRegistrationRequest.class);
        verify(lifecycleService).registerExecution(isNull(), captor.capture());
        ExecutionRegistrationRequest request = captor.getValue();
        assertThat(request.getRecommendationId()).isEqualTo(threadId);
        assertThat(request.getThreadId()).isNull();
        assertThat(request.getBefore()).isSameAs(baseline);
        assertThat(request.getExecutedAt()).isEqualTo(executedAt);
    }

    @Test
    void reviewRecommendationAnalyzesPendingReviewsBeforeCollectingBaseline() {
        LocalDateTime executedAt = LocalDateTime.of(2026, 6, 15, 10, 0);
        mockRecommendation(new MockAutomaticExecutionService.MockRecommendation(
                10003L,
                3L,
                RecommendationType.REVIEW,
                null,
                null,
                "convenience",
                true,
                executedAt
        ));
        when(metricCollector.collect(anyLong(), any(), any(), any(), any()))
                .thenReturn(new PeriodMetrics(null, null));
        when(lifecycleService.registerExecution(isNull(), any()))
                .thenReturn(VerificationExecutionResponse.builder().build());

        service.registerAutomatically(10003L);

        verify(reviewSentimentService).analyzePending(
                3L,
                executedAt.minusDays(14),
                executedAt
        );
        ArgumentCaptor<ExecutionRegistrationRequest> captor =
                ArgumentCaptor.forClass(ExecutionRegistrationRequest.class);
        verify(lifecycleService).registerExecution(isNull(), captor.capture());
        assertThat(captor.getValue().getRecommendationType())
                .isEqualTo(RecommendationType.REVIEW);
        assertThat(captor.getValue().getCondition().getTargetAspect())
                .isEqualTo("convenience");
    }

    @Test
    void salesCompletionCollectsPostExecutionPeriodAndCompletesLifecycle() {
        LocalDateTime executedAt = LocalDateTime.of(2026, 6, 15, 10, 0);
        mockRecommendation(new MockAutomaticExecutionService.MockRecommendation(
                10001L,
                1L,
                RecommendationType.SALES,
                14,
                17,
                null,
                true,
                executedAt
        ));
        PeriodMetrics after = new PeriodMetrics(
                new SalesMetrics(72_000.0, 5, 14_400.0, 50.0,
                        100.0, 1, 0, 93_000.0),
                null
        );
        when(metricCollector.collect(anyLong(), any(), any(), any(), any()))
                .thenReturn(after);
        when(lifecycleService.completeVerification(isNull(), anyString(), any()))
                .thenReturn(new EffectVerificationResponse());

        service.completeAutomatically(10001L);

        verify(metricCollector).collect(
                eq(1L),
                eq(RecommendationType.SALES),
                eq(executedAt),
                eq(executedAt.plusDays(14)),
                any()
        );
        ArgumentCaptor<VerificationCompletionRequest> captor =
                ArgumentCaptor.forClass(VerificationCompletionRequest.class);
        verify(lifecycleService).completeVerification(
                isNull(),
                eq("10001"),
                captor.capture()
        );
        verify(feedbackService).apply(eq(10001L), any());
        assertThat(captor.getValue().getAfter()).isSameAs(after);
        assertThat(captor.getValue().getCollectedAt())
                .isEqualTo(executedAt.plusDays(14));
    }

    @Test
    void reviewCompletionAnalyzesPostExecutionReviewsBeforeCollection() {
        LocalDateTime executedAt = LocalDateTime.of(2026, 6, 15, 10, 0);
        mockRecommendation(new MockAutomaticExecutionService.MockRecommendation(
                10003L,
                3L,
                RecommendationType.REVIEW,
                null,
                null,
                "convenience",
                true,
                executedAt
        ));
        when(metricCollector.collect(anyLong(), any(), any(), any(), any()))
                .thenReturn(new PeriodMetrics(null, null));
        when(lifecycleService.completeVerification(isNull(), anyString(), any()))
                .thenReturn(new EffectVerificationResponse());

        service.completeAutomatically(10003L);

        verify(reviewSentimentService).analyzePending(
                3L,
                executedAt,
                executedAt.plusDays(14)
        );
        verify(lifecycleService).completeVerification(
                isNull(),
                eq("10003"),
                any()
        );
        verify(feedbackService).apply(eq(10003L), any());
    }

    @Test
    void threadCompletionCollectsMetricsUsingStoredExecutionContext() {
        String threadId = "11111111-1111-1111-1111-111111111111";
        LocalDateTime executedAt = LocalDateTime.of(2026, 6, 15, 10, 0);
        LocalDateTime dueAt = executedAt.plusDays(14);
        VerificationCondition condition = new VerificationCondition(
                14, 14, 17, true, null
        );
        PeriodMetrics after = new PeriodMetrics(
                new SalesMetrics(72_000.0, 5, 14_400.0, 50.0,
                        100.0, 1, 0, 93_000.0),
                null
        );
        when(lifecycleService.getAutomaticCollectionContext(threadId))
                .thenReturn(new EffectVerificationLifecycleService.AutomaticCollectionContext(
                        1L,
                        RecommendationType.SALES,
                        condition,
                        executedAt,
                        dueAt
                ));
        when(metricCollector.collect(
                1L,
                RecommendationType.SALES,
                executedAt,
                dueAt,
                condition
        )).thenReturn(after);
        when(lifecycleService.completeVerification(isNull(), eq(threadId), any()))
                .thenReturn(new EffectVerificationResponse());

        service.completeThreadAutomatically(threadId);

        ArgumentCaptor<VerificationCompletionRequest> captor =
                ArgumentCaptor.forClass(VerificationCompletionRequest.class);
        verify(lifecycleService).completeVerification(
                isNull(),
                eq(threadId),
                captor.capture()
        );
        assertThat(captor.getValue().getAfter()).isSameAs(after);
        assertThat(captor.getValue().getCollectedAt()).isEqualTo(dueAt);
        verify(feedbackService, never()).apply(anyLong(), any());
    }

    @Test
    void resetDeletesOnlyMockVerificationDataAndLearnedWeights() {
        when(jdbcTemplate.update(MockAutomaticExecutionService.DELETE_MOCK_RESULTS_SQL))
                .thenReturn(3);
        when(jdbcTemplate.update(MockAutomaticExecutionService.DELETE_MOCK_EXECUTIONS_SQL))
                .thenReturn(3);
        when(jdbcTemplate.update("DELETE FROM MockRecommendationStrategyWeight"))
                .thenReturn(2);

        MockAutomaticExecutionService.MockResetResult result =
                service.resetVerificationData();

        assertThat(result.deletedResults()).isEqualTo(3);
        assertThat(result.deletedExecutions()).isEqualTo(3);
        assertThat(result.deletedStrategyWeights()).isEqualTo(2);
    }

    @SuppressWarnings("unchecked")
    private void mockRecommendation(
            MockAutomaticExecutionService.MockRecommendation recommendation
    ) {
        when(jdbcTemplate.queryForObject(
                anyString(),
                any(RowMapper.class),
                eq(recommendation.recommendationId())
        )).thenReturn(recommendation);
    }
}
