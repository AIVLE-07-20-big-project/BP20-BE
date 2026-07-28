package com.bp20.backend.api.ai.service;

import com.bp20.backend.api.ai.client.FastApiClient;
import com.bp20.backend.api.ai.domain.AiAnalysis;
import com.bp20.backend.api.ai.domain.AiRecommendationRun;
import com.bp20.backend.api.ai.domain.AiStoreProfile;
import com.bp20.backend.api.ai.dto.request.AgentRunResumeRequest;
import com.bp20.backend.api.ai.repository.AiAnalysisRepository;
import com.bp20.backend.api.ai.repository.AiRecommendationRunRepository;
import com.bp20.backend.api.ai.repository.AiStoreProfileRepository;
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
    private final AiStoreProfileRepository storeProfileRepository;
    private final ObjectMapper objectMapper;

    // AI 서비스는 202 + job_id를 즉시 반환한다(비동기 분석). 완료 여부는
    // getAnalysisJobStatus로 폴링해서 확인한다.
    public Map<String, Object> createAnalysis(Long userId, String storeId, MultipartFile file,
                                               String trdarCd, String svcIndutyCd, Integer yyquCd) {
        String[] resolvedCodes = resolveCodes(userId, trdarCd, svcIndutyCd);
        Map<String, Object> result = fastApiClient.createAnalysis(
                file, resolvedCodes[0], resolvedCodes[1], yyquCd, userId, storeId
        );
        requiredString(result, "job_id");
        return result;
    }

    // 잡 상태를 조회하고, 완료된 시점에만 AI 서비스에서 결과를 가져와 로컬에 저장한다
    // (같은 analysis_id가 이미 저장돼 있으면 다시 가져오지 않는다 — 폴링마다 반복 호출되므로).
    public Map<String, Object> getAnalysisJobStatus(Long userId, String jobId) {
        Map<String, Object> job = fastApiClient.getJobStatus(jobId, userId);
        if ("completed".equals(job.get("status"))) {
            String analysisId = requiredString(job, "analysis_id");
            if (analysisRepository.findByAnalysisIdAndUserId(analysisId, userId).isEmpty()) {
                Map<String, Object> analysis = fastApiClient.getAnalysisResult(analysisId, userId);
                analysisRepository.save(AiAnalysis.create(
                        analysisId, userId,
                        (String) analysis.get("store_id"),
                        requiredString(analysis, "trdar_cd"),
                        requiredString(analysis, "svc_induty_cd"),
                        asInteger(analysis.get("yyqu_cd")),
                        write(analysis)
                ));
            }
        }
        return job;
    }

    private Integer asInteger(Object value) {
        return value == null ? null : ((Number) value).intValue();
    }

    // 이번 요청에 코드가 왔으면 사용자의 최근 값으로 저장하고, 없으면 저장된 값을 자동으로 채운다
    private String[] resolveCodes(Long userId, String trdarCd, String svcIndutyCd) {
        if (trdarCd != null && !trdarCd.isBlank() && svcIndutyCd != null && !svcIndutyCd.isBlank()) {
            AiStoreProfile profile = storeProfileRepository.findById(userId)
                    .map(existing -> {
                        existing.updateCodes(trdarCd, svcIndutyCd);
                        return existing;
                    })
                    .orElseGet(() -> AiStoreProfile.create(userId, trdarCd, svcIndutyCd));
            storeProfileRepository.save(profile);
            return new String[]{trdarCd, svcIndutyCd};
        }
        AiStoreProfile profile = storeProfileRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND_STORE));
        return new String[]{profile.getTrdarCd(), profile.getSvcIndutyCd()};
    }

    public Map<String, Object> getAnalysis(Long userId, String analysisId) {
        return read(findAnalysis(userId, analysisId).getResultJson());
    }

    public Map<String, Object> createRecommendation(Long userId, String analysisId) {
        AiAnalysis analysis = findAnalysis(userId, analysisId);
        Map<String, Object> result = fastApiClient.createRecommendation(analysisId, userId);
        String threadId = requiredString(result, "thread_id");
        runRepository.save(AiRecommendationRun.create(
                threadId, analysisId, userId, analysis.getStoreId(), write(result)
        ));
        return result;
    }

    public List<Map<String, Object>> getRecommendations(Long userId, String storeId) {
        List<AiRecommendationRun> runs = (storeId != null && !storeId.isBlank())
                ? runRepository.findAllByUserIdAndStoreIdOrderByCreatedAtDesc(userId, storeId)
                : runRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
        return runs.stream()
                .map(run -> {
                    Map<String, Object> result = new LinkedHashMap<>(read(run.getResultJson()));
                    result.putIfAbsent("thread_id", run.getThreadId());
                    result.putIfAbsent("analysis_id", run.getAnalysisId());
                    result.put("store_id", run.getStoreId());
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
