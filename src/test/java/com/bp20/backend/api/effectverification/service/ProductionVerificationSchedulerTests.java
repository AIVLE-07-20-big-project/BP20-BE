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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductionVerificationSchedulerTests {

    @Mock
    private EffectVerificationExecutionRepository repository;

    @Mock
    private AutomaticVerificationProcessor processor;

    @Mock
    private EffectVerificationSchedulerLock schedulerLock;

    private ProductionVerificationScheduler scheduler;

    @BeforeEach
    void setUp() throws ReflectiveOperationException {
        scheduler = new ProductionVerificationScheduler(
                repository,
                processor,
                schedulerLock
        );
        setField("enabled", true);
        setField("maxAttempts", 3);
        lenient().when(schedulerLock.runWithLock(any())).thenAnswer(invocation -> {
            Runnable task = invocation.getArgument(0);
            task.run();
            return true;
        });
    }

    @Test
    void processesDueExecutionWhenCollectorIsAvailable() {
        EffectVerificationExecution execution = execution(0);
        when(processor.isAvailable()).thenReturn(true);
        when(repository
                .findByStatusInAndVerificationDueAtLessThanEqualOrderByVerificationDueAtAsc(
                        any(),
                        any()
                )).thenReturn(List.of(execution));

        scheduler.runScheduled();

        verify(processor).process(execution);
    }

    @Test
    void skipsQueryWhenCollectorIsUnavailable() {
        when(processor.isAvailable()).thenReturn(false);

        scheduler.runScheduled();

        verify(repository, never())
                .findByStatusInAndVerificationDueAtLessThanEqualOrderByVerificationDueAtAsc(
                        any(),
                        any()
                );
    }

    @Test
    void skipsExecutionAfterMaximumAttempts() {
        EffectVerificationExecution execution = execution(3);
        when(processor.isAvailable()).thenReturn(true);
        when(repository
                .findByStatusInAndVerificationDueAtLessThanEqualOrderByVerificationDueAtAsc(
                        any(),
                        any()
                )).thenReturn(List.of(execution));

        scheduler.runScheduled();

        verify(processor, never()).process(execution);
    }

    @Test
    void skipsProcessingWhenAnotherTaskOwnsSchedulerLock() {
        when(processor.isAvailable()).thenReturn(true);
        doReturn(false).when(schedulerLock).runWithLock(any());

        scheduler.runScheduled();

        verify(repository, never())
                .findByStatusInAndVerificationDueAtLessThanEqualOrderByVerificationDueAtAsc(
                        any(),
                        any()
                );
    }

    private EffectVerificationExecution execution(int attempts) {
        return EffectVerificationExecution.builder()
                .aiRecommendationId("thread-1")
                .status(VerificationStatus.COLLECTING)
                .attemptCount(attempts)
                .verificationDueAt(LocalDateTime.now().minusDays(1))
                .build();
    }

    private void setField(String name, Object value)
            throws ReflectiveOperationException {
        var field = ProductionVerificationScheduler.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(scheduler, value);
    }
}
