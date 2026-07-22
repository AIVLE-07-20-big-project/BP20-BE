package com.bp20.backend.api.ai.service;

import com.bp20.backend.api.ai.client.FastApiClient;
import com.bp20.backend.api.ai.domain.AiAnalysis;
import com.bp20.backend.api.ai.domain.AiRecommendationRun;
import com.bp20.backend.api.ai.dto.request.AgentRunResumeRequest;
import com.bp20.backend.api.ai.repository.AiAnalysisRepository;
import com.bp20.backend.api.ai.repository.AiRecommendationRunRepository;
import com.bp20.backend.global.exception.ApiException;
import com.bp20.backend.global.response.ErrorCode;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AiService {
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final FastApiClient fastApiClient;
    private final AiAnalysisRepository analysisRepository;
    private final AiRecommendationRunRepository runRepository;
    private final ObjectMapper objectMapper;

    public Map<String, Object> createAnalysis(Long userId, String storeId, MultipartFile file,
                                               String trdarCd, String svcIndutyCd, Integer yyquCd) {
        Map<String, Object> result = fastApiClient.createAnalysis(
                file, trdarCd, svcIndutyCd, yyquCd, userId, storeId
        );
        String analysisId = requiredString(result, "analysis_id");
        analysisRepository.save(AiAnalysis.create(
                analysisId, userId, storeId, trdarCd, svcIndutyCd, yyquCd, write(result)
        ));
        return result;
    }

    public Map<String, Object> getAnalysis(Long userId, String analysisId) {
        return read(findAnalysis(userId, analysisId).getResultJson());
    }

    public Map<String, Object> createRecommendation(Long userId, String analysisId) {
        findAnalysis(userId, analysisId);
        Map<String, Object> result = fastApiClient.createRecommendation(analysisId, userId);
        String threadId = requiredString(result, "thread_id");
        runRepository.save(AiRecommendationRun.create(threadId, analysisId, userId, write(result)));
        return result;
    }

    public List<Map<String, Object>> getRecommendations(Long userId) {
        return runRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(run -> {
                    Map<String, Object> result = new LinkedHashMap<>(read(run.getResultJson()));
                    result.putIfAbsent("thread_id", run.getThreadId());
                    result.putIfAbsent("analysis_id", run.getAnalysisId());
                    result.put("created_at", run.getCreatedAt());
                    result.put("updated_at", run.getUpdatedAt());
                    return result;
                })
                .toList();
    }

    public Map<String, Object> getAgentRun(Long userId, String threadId) {
        AiRecommendationRun run = findRun(userId, threadId);
        Map<String, Object> result = fastApiClient.getAgentRun(threadId, userId);
        run.updateResult(write(result));
        runRepository.save(run);
        return result;
    }

    public Map<String, Object> resumeAgentRun(
            Long userId, String threadId, AgentRunResumeRequest request
    ) {
        AiRecommendationRun run = findRun(userId, threadId);
        Map<String, Object> result = fastApiClient.resumeAgentRun(threadId, request, userId);
        run.updateResult(write(result));
        runRepository.save(run);
        return result;
    }

    private AiAnalysis findAnalysis(Long userId, String analysisId) {
        return analysisRepository.findByAnalysisIdAndUserId(analysisId, userId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND_AI_ANALYSIS));
    }

    private AiRecommendationRun findRun(Long userId, String threadId) {
        return runRepository.findByThreadIdAndUserId(threadId, userId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND_AI_AGENT_RUN));
    }

    private String requiredString(Map<String, Object> value, String key) {
        Object field = value.get(key);
        if (field == null || field.toString().isBlank()) {
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
        return field.toString();
    }

    private String write(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR, e);
        }
    }

    private Map<String, Object> read(String value) {
        try {
            return objectMapper.readValue(value, MAP_TYPE);
        } catch (Exception e) {
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR, e);
        }
    }
}
