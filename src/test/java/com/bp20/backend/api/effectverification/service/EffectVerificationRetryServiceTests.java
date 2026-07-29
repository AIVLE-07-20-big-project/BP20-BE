package com.bp20.backend.api.effectverification.service;

import com.bp20.backend.api.effectverification.domain.EffectVerificationExecution;
import com.bp20.backend.api.effectverification.domain.VerificationStatus;
import com.bp20.backend.api.effectverification.dto.response.VerificationExecutionResponse;
import com.bp20.backend.api.effectverification.repository.EffectVerificationExecutionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EffectVerificationRetryServiceTests {

    @Mock
    private EffectVerificationExecutionRepository executionRepository;

    @Mock
    private AutomaticVerificationProcessor processor;

    @Mock
    private EffectVerificationLifecycleService lifecycleService;

    @Test
    void retriesFailedExecution() {
        EffectVerificationRetryService service = service();
        EffectVerificationExecution execution = failedExecution(1);
        VerificationExecutionResponse expected =
                VerificationExecutionResponse.builder().build();
        when(executionRepository.findByAiRecommendationIdAndUserId(
                "recommendation-1",
                10L
        )).thenReturn(Optional.of(execution));
        when(processor.isAvailable()).thenReturn(true);
        when(lifecycleService.getExecution(10L, "recommendation-1"))
                .thenReturn(expected);

        VerificationExecutionResponse actual =
                service.retry(10L, "recommendation-1");

        assertThat(actual).isSameAs(expected);
        verify(processor).process(execution);
    }

    @Test
    void rejectsNonFailedExecution() {
        EffectVerificationRetryService service = service();
        EffectVerificationExecution execution = EffectVerificationExecution.builder()
                .aiRecommendationId("recommendation-1")
                .status(VerificationStatus.COLLECTING)
                .attemptCount(0)
                .build();
        when(executionRepository.findByAiRecommendationIdAndUserId(
                "recommendation-1",
                10L
        )).thenReturn(Optional.of(execution));

        assertThatThrownBy(() -> service.retry(10L, "recommendation-1"))
                .isInstanceOfSatisfying(
                        ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode())
                                .isEqualTo(HttpStatus.CONFLICT)
                );
        verify(processor, never()).process(execution);
    }

    @Test
    void rejectsRetryAfterMaximumAttempts() {
        EffectVerificationRetryService service = service();
        EffectVerificationExecution execution = failedExecution(3);
        when(executionRepository.findByAiRecommendationIdAndUserId(
                "recommendation-1",
                10L
        )).thenReturn(Optional.of(execution));

        assertThatThrownBy(() -> service.retry(10L, "recommendation-1"))
                .isInstanceOfSatisfying(
                        ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode())
                                .isEqualTo(HttpStatus.CONFLICT)
                );
        verify(processor, never()).process(execution);
    }

    private EffectVerificationRetryService service() {
        EffectVerificationRetryService service = new EffectVerificationRetryService(
                executionRepository,
                processor,
                lifecycleService
        );
        ReflectionTestUtils.setField(service, "maxAttempts", 3);
        return service;
    }

    private EffectVerificationExecution failedExecution(int attemptCount) {
        return EffectVerificationExecution.builder()
                .aiRecommendationId("recommendation-1")
                .status(VerificationStatus.FAILED)
                .attemptCount(attemptCount)
                .build();
    }
}
