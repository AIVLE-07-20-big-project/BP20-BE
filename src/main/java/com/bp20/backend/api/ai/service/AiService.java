package com.bp20.backend.api.ai.service;

import com.bp20.backend.api.ai.client.FastApiClient;
import com.bp20.backend.api.ai.domain.AiAnalysis;
import com.bp20.backend.api.ai.domain.AiRecommendationRun;
import com.bp20.backend.api.ai.domain.AiStoreProfile;
import com.bp20.backend.api.ai.dto.request.AgentRunResumeRequest;
import com.bp20.backend.api.ai.repository.AiAnalysisRepository;
import com.bp20.backend.api.ai.repository.AiRecommendationRunRepository;
import com.bp20.backend.api.ai.repository.AiStoreProfileRepository;
import com.bp20.backend.api.store.domain.Store;
import com.bp20.backend.api.store.repository.StoreRepository;
import com.bp20.backend.api.user.domain.User;
import com.bp20.backend.api.user.repository.UserRepository;
import com.bp20.backend.global.exception.ApiException;
import com.bp20.backend.global.response.ErrorCode;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.bp20.backend.global.storage.S3ObjectStorageService;
import java.util.UUID;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AiService {
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    // 상세 화면에서만 필요한 무거운 필드. 기본 응답에서는 제외하고 DB(result_json)에는 그대로 보존한다.
    // 대기중_승인은 프론트엔드 승인 화면이 기본 응답에서 직접 읽으므로 제외 대상에서 뺀다.
    private static final Set<String> DETAIL_ONLY_FIELDS = Set.of(
            "scm_result", "ope_result", "candidate_safety", "shadow_report", "rag_evidence"
    );

    private final FastApiClient fastApiClient;
    private final AiAnalysisRepository analysisRepository;
    private final AiRecommendationRunRepository runRepository;
    private final AiStoreProfileRepository storeProfileRepository;
    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final ObjectMapper objectMapper;
    private final S3ObjectStorageService s3ObjectStorageService;

    // AI 서비스는 202 + job_id를 즉시 반환한다(비동기 분석). 완료 여부는
    // getAnalysisJobStatus로 폴링해서 확인한다.
    @Transactional
    public Map<String, Object> createAnalysis(Long userId, String storeId, MultipartFile file,
                                               String trdarCd, String svcIndutyCd, Integer yyquCd) {
        Store resolvedStore = resolveStore(userId, storeId);
        String[] resolvedCodes = resolveCodes(userId, trdarCd, svcIndutyCd);
        String jobId = UUID.randomUUID().toString();
        S3ObjectStorageService.StoredObject stored = s3ObjectStorageService.uploadCsv(file, jobId);
        Map<String, Object> result;
        try {
            result = stored == null
                    ? fastApiClient.createAnalysis(file, resolvedCodes[0], resolvedCodes[1], yyquCd, userId,
                    resolvedStore.getId().toString())
                    : fastApiClient.createAnalysisFromS3(jobId, stored.key(), resolvedCodes[0], resolvedCodes[1],
                    yyquCd, userId, resolvedStore.getId().toString());
        } catch (RuntimeException exception) {
            s3ObjectStorageService.delete(stored);
            throw exception;
        }
        requiredString(result, "job_id");
        return result;
    }

    public Map<String, Object> getLocations() {
        return fastApiClient.getLocations();
    }

    public Map<String, Object> getIndustries() {
        return fastApiClient.getIndustries();
    }

    // 잡 상태를 조회하고, 완료된 시점에만 AI 서비스에서 결과를 가져와 로컬에 저장한다
    // (같은 analysis_id가 이미 저장돼 있으면 다시 가져오지 않는다 — 폴링마다 반복 호출되므로).
    @Transactional
    public Map<String, Object> getAnalysisJobStatus(Long userId, String jobId) {
        Map<String, Object> job = fastApiClient.getJobStatus(jobId, userId);
        if ("completed".equals(job.get("status"))) {
            String analysisId = requiredString(job, "analysis_id");
            if (analysisRepository.findByAnalysisIdAndUser_Id(analysisId, userId).isEmpty()) {
                Map<String, Object> analysis = fastApiClient.getAnalysisResult(analysisId, userId);
                User user = findUser(userId);
                Store store = findOptionalOwnedStore(userId, analysis.get("store_id"));
                analysisRepository.save(AiAnalysis.create(
                        analysisId,
                        user,
                        store,
                        requiredString(analysis, "trdar_cd"),
                        requiredString(analysis, "svc_induty_cd"),
                        asInteger(analysis.get("yyqu_cd")),
                        write(analysis)
                ));
            }
            // 새 분석 CSV가 저장된 매출 기간을 포함하면 승인 방안의 사후 피드백을 확정한다.
        }
        return job;
    }

    private Integer asInteger(Object value) {
        return value == null ? null : ((Number) value).intValue();
    }

    // 이번 요청에 코드가 왔으면 사용자의 최근 값으로 저장하고, 없으면 저장된 값을 자동으로 채운다
    private String[] resolveCodes(Long userId, String trdarCd, String svcIndutyCd) {
        if (trdarCd != null && !trdarCd.isBlank() && svcIndutyCd != null && !svcIndutyCd.isBlank()) {
            User user = findUser(userId);
            AiStoreProfile profile = storeProfileRepository.findById(userId)
                    .map(existing -> {
                        existing.updateCodes(trdarCd, svcIndutyCd);
                        return existing;
                    })
                    .orElseGet(() -> AiStoreProfile.create(user, trdarCd, svcIndutyCd));
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

    @Transactional
    public Map<String, Object> createRecommendation(Long userId, String analysisId) {
        AiAnalysis analysis = findAnalysis(userId, analysisId);
        Store store = analysis.getStore() != null
                ? analysis.getStore()
                : resolveStore(userId, null);
        if (analysis.getStore() == null) {
            analysis.attachStore(store);
            analysisRepository.save(analysis);
        }
        Map<String, Object> result = fastApiClient.createRecommendation(
                analysisId, userId, store.getId().toString()
        );
        String threadId = requiredString(result, "thread_id");
        runRepository.save(AiRecommendationRun.create(
                threadId,
                analysis,
                analysis.getUser(),
                write(result)
        ));
        return summarize(result);
    }

    public List<Map<String, Object>> getRecommendations(Long userId) {
        List<AiRecommendationRun> runs =
                runRepository.findAllByUser_IdOrderByCreatedAtDesc(userId);
        return runs.stream()
                .map(run -> {
                    Map<String, Object> result = summarize(read(run.getResultJson()));
                    result.putIfAbsent("thread_id", run.getThreadId());
                    result.putIfAbsent("analysis_id", run.getAnalysis().getAnalysisId());
                    result.put("created_at", run.getCreatedAt());
                    result.put("updated_at", run.getUpdatedAt());
                    if (run.getExecutionStartedAt() != null) {
                        result.put("execution_started_at", run.getExecutionStartedAt());
                    }
                    if (run.getExecutionEndedAt() != null) {
                        result.put("execution_ended_at", run.getExecutionEndedAt());
                    }
                    return result;
                })
                .toList();
    }

    @Transactional
    public Map<String, Object> getAgentRun(Long userId, String threadId) {
        AiRecommendationRun run = findRun(userId, threadId);
        Map<String, Object> result = fastApiClient.getAgentRun(threadId, userId);
        run.updateResult(write(result));
        runRepository.save(run);
        Map<String, Object> summary = summarize(result);
        if (run.getExecutionStartedAt() != null) {
            summary.put("execution_started_at", run.getExecutionStartedAt());
            summary.put("execution_ended_at", run.getExecutionEndedAt());
        }
        return summary;
    }

    // 상세 화면 전용: scm_result/ope_result 등 무거운 필드를 포함한 원본 전체를 반환한다.
    // 새 조회 없이 이미 DB(result_json)에 저장된 최신 결과를 그대로 읽는다.
    public Map<String, Object> getAgentRunDetail(Long userId, String threadId) {
        return read(findRun(userId, threadId).getResultJson());
    }

    @Transactional
    public Map<String, Object> resumeAgentRun(
            Long userId, String threadId, AgentRunResumeRequest request
    ) {
        AiRecommendationRun run = findRun(userId, threadId);
        Map<String, Object> result = fastApiClient.resumeAgentRun(threadId, request, userId);
        if (request.decision() == AgentRunResumeRequest.Decision.approve
                && request.executionStartedAt() == null && request.executionEndedAt() == null) {
            LocalDateTime startedAt = LocalDateTime.now();
            run.updateExecutionPlan(startedAt, startedAt.plusDays(30),
                    request.selectedAction() != null ? request.selectedAction()
                            : request.modificationPlan() != null ? request.modificationPlan()
                            : run.getSelectedAction());
        } else if (request.decision() == AgentRunResumeRequest.Decision.approve
                && request.executionStartedAt() != null && request.executionEndedAt() != null) {
            run.updateExecutionPlan(
                    request.executionStartedAt(), request.executionEndedAt(),
                    request.selectedAction() != null ? request.selectedAction() : request.modificationPlan()
            );
        }
        run.updateResult(write(result));
        runRepository.save(run);
        return summarize(result);
    }

    // 무거운 필드를 제외한 요약 Map을 새로 만든다. 원본(result)은 그대로 두고 복사본만 걸러낸다
    // — DB에는 항상 원본 전체가 저장돼야 재현·감사가 가능하기 때문이다.
    private Map<String, Object> summarize(Map<String, Object> full) {
        Map<String, Object> summary = new LinkedHashMap<>(full);
        summary.keySet().removeAll(DETAIL_ONLY_FIELDS);
        return summary;
    }

    private AiAnalysis findAnalysis(Long userId, String analysisId) {
        return analysisRepository.findByAnalysisIdAndUser_Id(analysisId, userId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND_AI_ANALYSIS));
    }

    private AiRecommendationRun findRun(Long userId, String threadId) {
        return runRepository.findByThreadIdAndUser_Id(threadId, userId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND_AI_AGENT_RUN));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND_USER));
    }

    private Store findOptionalOwnedStore(Long userId, Object storeId) {
        if (storeId == null || storeId.toString().isBlank()) {
            return null;
        }
        return findOwnedStore(userId, storeId.toString());
    }

    private Store findOwnedStore(Long userId, String storeId) {
        try {
            return storeRepository.findByIdAndOwnerId(Long.valueOf(storeId), userId)
                    .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND_STORE));
        } catch (NumberFormatException exception) {
            throw new ApiException(ErrorCode.NOT_FOUND_STORE, exception);
        }
    }

    private Store resolveStore(Long userId, String storeId) {
        if (storeId != null && !storeId.isBlank()) {
            return findOwnedStore(userId, storeId);
        }
        return storeRepository.findByOwnerId(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND_STORE));
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
