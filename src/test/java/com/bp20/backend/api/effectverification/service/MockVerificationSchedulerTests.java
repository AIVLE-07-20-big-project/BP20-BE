package com.bp20.backend.api.effectverification.service;

import com.bp20.backend.api.effectverification.domain.EffectVerificationExecution;
import com.bp20.backend.api.effectverification.domain.VerificationStatus;
import com.bp20.backend.api.effectverification.repository.EffectVerificationExecutionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MockVerificationSchedulerTests {

    @Mock
    private EffectVerificationExecutionRepository executionRepository;

    @Mock
    private MockAutomaticExecutionService automaticExecutionService;

    private MockVerificationScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new MockVerificationScheduler(
                executionRepository,
                automaticExecutionService
        );
        setField("maxAttempts", 3);
    }

    @Test
    void runDueVerificationsCompletesEligibleExecutionAndSkipsMaxAttempts() {
        EffectVerificationExecution eligible = execution(10001L, 1);
        EffectVerificationExecution exhausted = execution(10002L, 3);
        when(executionRepository
                .findByStatusInAndVerificationDueAtLessThanEqualOrderByVerificationDueAtAsc(
                        any(),
                        any()
                )).thenReturn(List.of(eligible, exhausted));
        when(automaticExecutionService.supportsRecommendation(any()))
                .thenReturn(true);

        var response = scheduler.runDueVerifications();

        assertThat(response.processed()).isEqualTo(2);
        assertThat(response.succeeded()).isEqualTo(1);
        assertThat(response.skipped()).isEqualTo(1);
        verify(automaticExecutionService).completeAutomatically(10001L);
        verify(automaticExecutionService, never()).completeAutomatically(10002L);
    }

    @Test
    void runDueVerificationsContinuesWhenOneExecutionFails() {
        EffectVerificationExecution first = execution(10001L, 0);
        EffectVerificationExecution second = execution(10002L, 0);
        when(executionRepository
                .findByStatusInAndVerificationDueAtLessThanEqualOrderByVerificationDueAtAsc(
                        any(),
                        any()
                )).thenReturn(List.of(first, second));
        when(automaticExecutionService.supportsRecommendation(any()))
                .thenReturn(true);
        when(automaticExecutionService.completeAutomatically(10001L))
                .thenThrow(new IllegalStateException("AI unavailable"));

        var response = scheduler.runDueVerifications();

        assertThat(response.failed()).isEqualTo(1);
        assertThat(response.succeeded()).isEqualTo(1);
        verify(automaticExecutionService).completeAutomatically(10002L);
    }

    @Test
    void runDueVerificationsSkipsExecutionsOutsideMockRecommendations() {
        EffectVerificationExecution execution = execution(999L, 0);
        when(executionRepository
                .findByStatusInAndVerificationDueAtLessThanEqualOrderByVerificationDueAtAsc(
                        any(),
                        any()
                )).thenReturn(List.of(execution));
        when(automaticExecutionService.supportsRecommendation(999L))
                .thenReturn(false);

        var response = scheduler.runDueVerifications();

        assertThat(response.processed()).isEqualTo(1);
        assertThat(response.skipped()).isEqualTo(1);
        verify(automaticExecutionService, never()).completeAutomatically(999L);
    }

    private EffectVerificationExecution execution(Long recommendationId, int attempts) {
        return EffectVerificationExecution.builder()
                .aiRecommendationId(recommendationId)
                .storeId(1L)
                .status(VerificationStatus.COLLECTING)
                .attemptCount(attempts)
                .verificationDueAt(LocalDateTime.now().minusDays(1))
                .build();
    }

    private void setField(String name, Object value) {
        try {
            var field = MockVerificationScheduler.class.getDeclaredField(name);
            field.setAccessible(true);
            field.set(scheduler, value);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }
}
