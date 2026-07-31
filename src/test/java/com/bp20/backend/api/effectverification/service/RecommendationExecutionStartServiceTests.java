package com.bp20.backend.api.effectverification.service;

import com.bp20.backend.api.effectverification.client.CampaignExecutionClient;
import com.bp20.backend.api.effectverification.collector.VerificationMetricCollector;
import com.bp20.backend.api.effectverification.dto.request.ExecutionRegistrationRequest;
import com.bp20.backend.api.effectverification.dto.request.PeriodMetrics;
import com.bp20.backend.api.effectverification.dto.request.RecommendationExecutionStartRequest;
import com.bp20.backend.api.effectverification.dto.request.RecommendationType;
import com.bp20.backend.api.effectverification.dto.request.VerificationCondition;
import com.bp20.backend.api.effectverification.dto.response.VerificationExecutionResponse;
import com.bp20.backend.api.effectverification.repository.EffectVerificationExecutionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationExecutionStartServiceTests {

    @Mock
    private ObjectProvider<VerificationMetricCollector> collectorProvider;

    @Mock
    private VerificationMetricCollector collector;

    @Mock
    private CampaignExecutionClient campaignExecutionClient;

    @Mock
    private EffectVerificationExecutionRepository executionRepository;

    @Mock
    private EffectVerificationLifecycleService lifecycleService;

    @Mock
    private EffectVerificationStoreAccessService storeAccessService;

    private final Clock clock = Clock.fixed(
            Instant.parse("2026-07-29T05:00:00Z"),
            ZoneId.of("Asia/Seoul")
    );

    @Test
    void startsSalesVerificationFromCampaignExecutionResult() {
        RecommendationExecutionStartService service = service();
        RecommendationExecutionStartRequest input = request();
        PeriodMetrics before = new PeriodMetrics();
        VerificationExecutionResponse expected =
                VerificationExecutionResponse.builder().build();
        when(executionRepository.existsByRecommendationRun_ThreadId("thread-uuid"))
                .thenReturn(false);
        when(collectorProvider.getIfAvailable()).thenReturn(collector);
        when(campaignExecutionClient.recordExecution(
                eq("thread-uuid"),
                eq(10L),
                eq(20263),
                any(LocalDateTime.class)
        )).thenReturn(Map.of(
                "store_id", "1",
                "action_id", "쿠폰발행"
        ));
        when(collector.collect(
                eq(1L),
                eq(RecommendationType.SALES),
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                any(VerificationCondition.class)
        )).thenReturn(before);
        when(lifecycleService.registerExecution(
                eq(10L),
                any(ExecutionRegistrationRequest.class)
        )).thenReturn(expected);

        VerificationExecutionResponse actual = service.start(10L, input);

        LocalDateTime executedAt =
                LocalDateTime.of(2026, 7, 29, 14, 0);
        ArgumentCaptor<ExecutionRegistrationRequest> requestCaptor =
                ArgumentCaptor.forClass(ExecutionRegistrationRequest.class);
        verify(lifecycleService).registerExecution(
                eq(10L),
                requestCaptor.capture()
        );
        ExecutionRegistrationRequest registration = requestCaptor.getValue();
        assertThat(actual).isSameAs(expected);
        assertThat(registration.getThreadId()).isEqualTo("thread-uuid");
        assertThat(registration.getDecisionId()).isNull();
        assertThat(registration.getStoreId()).isEqualTo(1L);
        assertThat(registration.getRecommendationType())
                .isEqualTo(RecommendationType.SALES);
        assertThat(registration.getExecutedAt()).isEqualTo(executedAt);
        assertThat(registration.getBefore()).isSameAs(before);
        assertThat(registration.getCondition().getPeriodDays()).isEqualTo(30);
        assertThat(registration.getCondition().getStartHour()).isNull();
        assertThat(registration.getCondition().getEndHour()).isNull();
        assertThat(registration.getSelectedAction().getAction())
                .isEqualTo("쿠폰발행");
        assertThat(registration.getSelectedAction().getAxis()).isNull();
        verify(collector).collect(
                1L,
                RecommendationType.SALES,
                executedAt.minusDays(30),
                executedAt,
                registration.getCondition()
        );
    }

    @Test
    void startsReviewVerificationWithTargetAspect() {
        RecommendationExecutionStartService service = service();
        RecommendationExecutionStartRequest input = request();
        input.setRecommendationType(RecommendationType.REVIEW);
        input.setTargetAspect("convenience");
        PeriodMetrics before = new PeriodMetrics();
        when(executionRepository.existsByRecommendationRun_ThreadId("thread-uuid"))
                .thenReturn(false);
        when(collectorProvider.getIfAvailable()).thenReturn(collector);
        when(campaignExecutionClient.recordExecution(
                eq("thread-uuid"),
                eq(10L),
                eq(20263),
                any(LocalDateTime.class)
        )).thenReturn(Map.of(
                "decision_id", "decision-uuid",
                "store_id", 3,
                "action_id", "대기시간 개선"
        ));
        when(collector.collect(
                eq(3L),
                eq(RecommendationType.REVIEW),
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                any(VerificationCondition.class)
        )).thenReturn(before);

        service.start(10L, input);

        ArgumentCaptor<ExecutionRegistrationRequest> captor =
                ArgumentCaptor.forClass(ExecutionRegistrationRequest.class);
        verify(lifecycleService).registerExecution(eq(10L), captor.capture());
        assertThat(captor.getValue().getRecommendationType())
                .isEqualTo(RecommendationType.REVIEW);
        assertThat(captor.getValue().getCondition().getTargetAspect())
                .isEqualTo("convenience");
    }

    @Test
    void rejectsReviewVerificationWithoutTargetAspectBeforeCampaignExecution() {
        RecommendationExecutionStartService service = service();
        RecommendationExecutionStartRequest input = request();
        input.setRecommendationType(RecommendationType.REVIEW);
        when(executionRepository.existsByRecommendationRun_ThreadId("thread-uuid"))
                .thenReturn(false);
        when(collectorProvider.getIfAvailable()).thenReturn(collector);

        assertThatThrownBy(() -> service.start(10L, input))
                .isInstanceOfSatisfying(
                        ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode())
                                .isEqualTo(HttpStatus.BAD_REQUEST)
                );
        verify(campaignExecutionClient, never())
                .recordExecution(any(), any(), anyInt(), any());
    }

    @Test
    void returnsExistingExecutionWithoutRecordingCampaignAgain() {
        RecommendationExecutionStartService service = service();
        VerificationExecutionResponse expected =
                VerificationExecutionResponse.builder().build();
        when(executionRepository.existsByRecommendationRun_ThreadId("thread-uuid"))
                .thenReturn(true);
        when(lifecycleService.getExecution(10L, "thread-uuid"))
                .thenReturn(expected);

        VerificationExecutionResponse actual = service.start(10L, request());

        assertThat(actual).isSameAs(expected);
        verify(campaignExecutionClient, never())
                .recordExecution(any(), any(), anyInt(), any());
        verify(collectorProvider, never()).getIfAvailable();
    }

    @Test
    void returnsServiceUnavailableBeforeCampaignWhenCollectorIsMissing() {
        RecommendationExecutionStartService service = service();
        when(executionRepository.existsByRecommendationRun_ThreadId("thread-uuid"))
                .thenReturn(false);
        when(collectorProvider.getIfAvailable()).thenReturn(null);

        assertThatThrownBy(() -> service.start(10L, request()))
                .isInstanceOfSatisfying(
                        ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode())
                                .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
                );
        verify(campaignExecutionClient, never())
                .recordExecution(any(), any(), anyInt(), any());
        verify(lifecycleService, never())
                .registerExecution(any(), any());
    }

    private RecommendationExecutionStartService service() {
        return new RecommendationExecutionStartService(
                collectorProvider,
                campaignExecutionClient,
                executionRepository,
                lifecycleService,
                storeAccessService,
                clock
        );
    }

    private RecommendationExecutionStartRequest request() {
        RecommendationExecutionStartRequest request =
                new RecommendationExecutionStartRequest();
        request.setThreadId("thread-uuid");
        return request;
    }
}
