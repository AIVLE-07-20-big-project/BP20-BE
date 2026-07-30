package com.bp20.backend.api.effectverification.service;

import com.bp20.backend.api.effectverification.collector.VerificationMetricCollector;
import com.bp20.backend.api.effectverification.domain.EffectVerificationExecution;
import com.bp20.backend.api.effectverification.dto.request.PeriodMetrics;
import com.bp20.backend.api.effectverification.dto.request.RecommendationType;
import com.bp20.backend.api.effectverification.dto.request.VerificationCondition;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.ObjectProvider;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AutomaticVerificationProcessorTests {

    @Test
    void collectsPostExecutionMetricsAndCompletesVerification() {
        VerificationMetricCollector collector = mock(VerificationMetricCollector.class);
        EffectVerificationLifecycleService lifecycle = mock(
                EffectVerificationLifecycleService.class
        );
        ObjectProvider<VerificationMetricCollector> provider = provider(collector);
        AutomaticVerificationProcessor processor =
                new AutomaticVerificationProcessor(provider, lifecycle);
        LocalDateTime from = LocalDateTime.of(2026, 7, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 7, 15, 0, 0);
        VerificationCondition condition = new VerificationCondition(
                14, null, null, false, null
        );
        PeriodMetrics metrics = new PeriodMetrics();
        when(lifecycle.getAutomaticCollectionContext("thread-1"))
                .thenReturn(new EffectVerificationLifecycleService.AutomaticCollectionContext(
                        1L,
                        RecommendationType.SALES,
                        condition,
                        from,
                        to
                ));
        when(collector.collect(1L, RecommendationType.SALES, from, to, condition))
                .thenReturn(metrics);

        processor.process(EffectVerificationExecution.builder()
                .aiRecommendationId("thread-1")
                .build());

        verify(lifecycle).completeVerification(
                eq(null),
                eq("thread-1"),
                any()
        );
        assertThat(processor.isAvailable()).isTrue();
    }

    private ObjectProvider<VerificationMetricCollector> provider(
            VerificationMetricCollector collector
    ) {
        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
        factory.registerSingleton("collector", collector);
        return factory.getBeanProvider(VerificationMetricCollector.class);
    }
}
