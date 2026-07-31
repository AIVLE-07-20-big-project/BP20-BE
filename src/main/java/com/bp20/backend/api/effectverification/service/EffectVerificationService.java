package com.bp20.backend.api.effectverification.service;

import com.bp20.backend.api.effectverification.client.EffectVerificationApiClient;
import com.bp20.backend.api.effectverification.dto.request.EffectVerificationFromAnalysesRequest;
import com.bp20.backend.api.effectverification.dto.request.EffectVerificationRequest;
import com.bp20.backend.api.effectverification.dto.response.EffectVerificationFromAnalysesResponse;
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
import java.util.Map;

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
        saveResult(userId, response, response.getRecommendationId());
        return response;
    }

    /** 저장된 적용전·적용후 분석(analysis_id) 두 건을 AI에 넘겨 매출형 전략검증을 수행한다. */
    @Transactional
    public EffectVerificationResponse verifyEffectFromAnalyses(
            Long userId,
            String beforeAnalysisId,
            String afterAnalysisId,
            Long storeId,
            Long recommendationId,
            String resultRecommendationId,
            Integer startHour,
            Integer endHour
    ) {
        EffectVerificationFromAnalysesRequest request = new EffectVerificationFromAnalysesRequest(
                beforeAnalysisId, afterAnalysisId, storeId, recommendationId, startHour, endHour, null
        );
        EffectVerificationFromAnalysesResponse response =
                effectVerificationApiClient.verifyEffectFromAnalyses(request);
        // AI 응답의 recommendation_id는 숫자(내부 실행 ID)라 BE 저장 키(문자열 thread_id 등)와 다르다 —
        // 저장·조회는 항상 이 문자열 키를 기준으로 한다.
        saveResult(userId, response, resultRecommendationId);
        return response;
    }

    private void saveResult(
            Long userId,
            EffectVerificationResponse response,
            String resultRecommendationId
    ) {
        LocalDateTime verifiedDate = LocalDateTime.now();
        String metricResults = writeMetricResults(response.getMetricResults());
        String strategyReportJson = writeStrategyReport(response.getStrategyReport());

        EffectVerificationResult result = resultRepository
                .findByAiRecommendationIdAndUserId(
                        resultRecommendationId,
                        userId
                )
                .orElseGet(() -> EffectVerificationResult.builder()
                        .aiRecommendationId(resultRecommendationId)
                        .userId(userId)
                        .build());

        result.update(
                response.getStoreId(),
                response.getRecommendationType(),
                response.getEffectScore(),
                response.getVerdict(),
                metricResults,
                response.getSummary(),
                strategyReportJson,
                verifiedDate
        );
        resultRepository.save(result);
        response.setVerifiedDate(verifiedDate);
    }

    @Transactional(readOnly = true)
    public EffectVerificationResponse getByRecommendationId(
            Long userId,
            String recommendationId
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
        response.setStrategyReport(readStrategyReport(result.getStrategyReportJson()));
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

    private String writeStrategyReport(Map<String, Object> strategyReport) {
        if (strategyReport == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(strategyReport);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize strategy report", e);
        }
    }

    private Map<String, Object> readStrategyReport(String strategyReportJson) {
        if (strategyReportJson == null) {
            return null;
        }
        try {
            return objectMapper.readValue(
                    strategyReportJson,
                    new TypeReference<Map<String, Object>>() { }
            );
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize strategy report", e);
        }
    }
}
