package com.bp20.backend.api.effectverification.service;

import com.bp20.backend.api.effectverification.client.EffectVerificationApiClient;
import com.bp20.backend.api.effectverification.dto.request.EffectVerificationRequest;
import com.bp20.backend.api.effectverification.dto.request.RecommendationType;
import com.bp20.backend.api.effectverification.dto.response.EffectVerificationResponse;
import com.bp20.backend.api.effectverification.dto.response.MetricResult;
import com.bp20.backend.api.effectverification.domain.EffectVerificationResult;
import com.bp20.backend.api.effectverification.repository.EffectVerificationResultRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EffectVerificationServiceTests {

    private static final long USER_ID = 7L;

    @Mock
    private EffectVerificationApiClient apiClient;

    @Mock
    private EffectVerificationResultRepository resultRepository;

    private EffectVerificationService service;

    @BeforeEach
    void setUp() {
        service = new EffectVerificationService(
                apiClient,
                resultRepository
        );
    }

    @Test
    void verifyEffectSavesAiResponse() {
        EffectVerificationRequest request = new EffectVerificationRequest();
        EffectVerificationResponse aiResponse = createResponse();
        when(apiClient.verifyEffect(request)).thenReturn(aiResponse);
        when(resultRepository.findByAiRecommendationIdAndUserId(1L, USER_ID))
                .thenReturn(Optional.empty());
        when(resultRepository.save(any(EffectVerificationResult.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        EffectVerificationResponse response = service.verifyEffect(USER_ID, request);

        ArgumentCaptor<EffectVerificationResult> captor =
                ArgumentCaptor.forClass(EffectVerificationResult.class);
        verify(resultRepository).save(captor.capture());
        EffectVerificationResult saved = captor.getValue();

        assertThat(saved.getAiRecommendationId()).isEqualTo(1L);
        assertThat(saved.getUserId()).isEqualTo(USER_ID);
        assertThat(saved.getStoreId()).isEqualTo(10L);
        assertThat(saved.getRecommendationType()).isEqualTo(RecommendationType.SALES);
        assertThat(saved.getEffectScore()).isEqualTo(96.0);
        assertThat(saved.getMetricResults()).contains("target_sales");
        assertThat(response.getVerifiedDate()).isNotNull();
    }

    @Test
    void getByRecommendationIdRestoresSavedResponse() throws Exception {
        EffectVerificationResponse original = createResponse();
        LocalDateTime verifiedDate = LocalDateTime.of(2026, 7, 15, 15, 30);
        String metricsJson = new ObjectMapper()
                .writeValueAsString(original.getMetricResults());
        EffectVerificationResult saved = EffectVerificationResult.builder()
                .aiRecommendationId(1L)
                .userId(USER_ID)
                .storeId(10L)
                .recommendationType(RecommendationType.SALES)
                .effectScore(96.0)
                .verdict("EFFECTIVE")
                .metricResults(metricsJson)
                .summary("success")
                .verifiedDate(verifiedDate)
                .build();
        when(resultRepository.findByAiRecommendationIdAndUserId(1L, USER_ID))
                .thenReturn(Optional.of(saved));

        EffectVerificationResponse response =
                service.getByRecommendationId(USER_ID, 1L);

        assertThat(response.getRecommendationId()).isEqualTo(1L);
        assertThat(response.getStoreId()).isEqualTo(10L);
        assertThat(response.getMetricResults()).hasSize(1);
        assertThat(response.getMetricResults().getFirst().getMetricName())
                .isEqualTo("target_sales");
        assertThat(response.getVerifiedDate()).isEqualTo(verifiedDate);
    }

    private EffectVerificationResponse createResponse() {
        MetricResult metric = new MetricResult();
        metric.setMetricName("target_sales");
        metric.setBeforeValue(1_000_000.0);
        metric.setAfterValue(1_300_000.0);
        metric.setChangeValue(300_000.0);
        metric.setChangeRate(30.0);
        metric.setImproved(true);

        EffectVerificationResponse response = new EffectVerificationResponse();
        response.setStoreId(10L);
        response.setRecommendationId(1L);
        response.setRecommendationType(RecommendationType.SALES);
        response.setEffectScore(96.0);
        response.setVerdict("EFFECTIVE");
        response.setMetricResults(List.of(metric));
        response.setSummary("success");
        return response;
    }
}
