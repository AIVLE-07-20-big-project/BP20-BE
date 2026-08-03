package com.bp20.backend.api.effectverification.service;

import com.bp20.backend.api.effectverification.client.EffectVerificationApiClient;
import com.bp20.backend.api.effectverification.dto.request.EffectVerificationRequest;
import com.bp20.backend.api.effectverification.dto.response.MetricResult;
import com.bp20.backend.api.effectverification.dto.response.EffectVerificationResponse;
import com.bp20.backend.api.effectverification.domain.EffectVerificationResult;
import com.bp20.backend.api.effectverification.domain.EffectVerificationExecution;
import com.bp20.backend.api.effectverification.repository.EffectVerificationExecutionRepository;
import com.bp20.backend.api.effectverification.repository.EffectVerificationResultRepository;
import com.bp20.backend.api.store.domain.Store;
import com.bp20.backend.api.store.repository.StoreRepository;
import com.bp20.backend.api.user.domain.User;
import com.bp20.backend.api.user.repository.UserRepository;
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
    private final EffectVerificationExecutionRepository executionRepository;
    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public EffectVerificationResponse verifyEffect(
            Long userId,
            EffectVerificationRequest request
    ) {
        EffectVerificationResponse response = effectVerificationApiClient.verifyEffect(request);
        LocalDateTime verifiedDate = LocalDateTime.now();
        String metricResults = writeMetricResults(response.getMetricResults());
        User user = userId == null
                ? null
                : userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));
        Store store = storeRepository.findById(response.getStoreId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Store not found"
                ));
        EffectVerificationExecution execution = executionRepository
                .findByAiRecommendationId(response.getRecommendationId())
                .orElse(null);

        EffectVerificationResult result = resultRepository
                .findByAiRecommendationIdAndUser_Id(
                        response.getRecommendationId(),
                        userId
                )
                .orElseGet(() -> EffectVerificationResult.builder()
                        .aiRecommendationId(response.getRecommendationId())
                        .execution(execution)
                        .user(user)
                        .store(store)
                        .build());

        result.update(
                store,
                response.getRecommendationType(),
                response.getEffectScore(),
                response.getVerdict(),
                metricResults,
                response.getSummary(),
                verifiedDate
        );
        if (execution != null) {
            result.linkExecution(execution);
        }
        resultRepository.save(result);
        response.setVerifiedDate(verifiedDate);

        return response;
    }

    @Transactional(readOnly = true)
    public EffectVerificationResponse getByRecommendationId(
            Long userId,
            String recommendationId
    ) {
        EffectVerificationResult result = resultRepository
                .findByAiRecommendationIdAndUser_Id(recommendationId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Effect verification result not found"
                ));

        EffectVerificationResponse response = new EffectVerificationResponse();
        response.setStoreId(result.getStore().getId());
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
