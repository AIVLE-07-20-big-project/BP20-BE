package com.bp20.backend.api.effectverification.service;

import com.bp20.backend.api.effectverification.client.EffectVerificationApiClient;
import com.bp20.backend.api.effectverification.dto.request.EffectVerificationRequest;
import com.bp20.backend.api.effectverification.dto.response.MetricResult;
import com.bp20.backend.api.effectverification.dto.response.EffectVerificationResponse;
import com.bp20.backend.api.effectverification.domain.EffectVerificationResult;
import com.bp20.backend.api.effectverification.repository.EffectVerificationResultRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EffectVerificationService {

    private final EffectVerificationApiClient effectVerificationApiClient;
    private final EffectVerificationResultRepository resultRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public EffectVerificationResponse verifyEffect(
            Long userId,
            EffectVerificationRequest request
    ) {
        EffectVerificationResponse response = effectVerificationApiClient.verifyEffect(request);
        LocalDateTime verifiedDate = LocalDateTime.now();
        String metricResults = writeMetricResults(response.getMetricResults());

        EffectVerificationResult result = resultRepository
                .findByAiRecommendationIdAndUserId(
                        response.getRecommendationId(),
                        userId
                )
                .orElseGet(() -> EffectVerificationResult.builder()
                        .aiRecommendationId(response.getRecommendationId())
                        .userId(userId)
                        .build());

        result.update(
                response.getStoreId(),
                response.getRecommendationType(),
                response.getEffectScore(),
                response.getVerdict(),
                metricResults,
                response.getSummary(),
                verifiedDate
        );
        resultRepository.save(result);
        response.setVerifiedDate(verifiedDate);

        return response;
    }

    @Transactional(readOnly = true)
    public EffectVerificationResponse getByRecommendationId(
            Long userId,
            Long recommendationId
    ) {
        EffectVerificationResult result = resultRepository
                .findByAiRecommendationIdAndUserId(recommendationId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Effect verification result not found"
                ));

        EffectVerificationResponse response = new EffectVerificationResponse();
        response.setStoreId(result.getStoreId());
        response.setRecommendationId(result.getAiRecommendationId());
        response.setRecommendationType(result.getRecommendationType());
        response.setEffectScore(result.getEffectScore());
        response.setVerdict(result.getVerdict());
        response.setMetricResults(readMetricResults(result.getMetricResults()));
        response.setSummary(result.getSummary());
        response.setVerifiedDate(result.getVerifiedDate());
        return response;
    }

    private String writeMetricResults(List<MetricResult> metricResults) {
        try {
            return objectMapper.writeValueAsString(metricResults);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize metric results", e);
        }
    }

    private List<MetricResult> readMetricResults(String metricResults) {
        try {
            return objectMapper.readValue(
                    metricResults,
                    new TypeReference<List<MetricResult>>() { }
            );
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize metric results", e);
        }
    }
}
