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
import com.bp20.backend.global.storage.S3ObjectStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiServiceTest {
    private FastApiClient client;
    private AiAnalysisRepository analysisRepository;
    private AiRecommendationRunRepository runRepository;
    private AiStoreProfileRepository storeProfileRepository;
    private UserRepository userRepository;
    private StoreRepository storeRepository;
    private S3ObjectStorageService s3ObjectStorageService;
    private User user;
    private Store store;
    private AiService service;

    @BeforeEach
    void setUp() {
        client = mock(FastApiClient.class);
        analysisRepository = mock(AiAnalysisRepository.class);
        runRepository = mock(AiRecommendationRunRepository.class);
        storeProfileRepository = mock(AiStoreProfileRepository.class);
        userRepository = mock(UserRepository.class);
        storeRepository = mock(StoreRepository.class);
        s3ObjectStorageService = mock(S3ObjectStorageService.class);
        user = mock(User.class);
        store = mock(Store.class);
        when(user.getId()).thenReturn(7L);
        when(store.getId()).thenReturn(1L);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(storeRepository.findByIdAndOwnerId(1L, 7L)).thenReturn(Optional.of(store));
        service = new AiService(
                client,
                analysisRepository,
                runRepository,
                storeProfileRepository,
                userRepository,
                storeRepository,
                JsonMapper.builder().build(),
                s3ObjectStorageService
        );
    }

    @Test
    void createAnalysisReturnsJobAndSavesStoreProfileWithoutPersistingAnalysisYet() {
        MockMultipartFile file = new MockMultipartFile("file", "sales.csv", "text/csv", new byte[0]);
        Map<String, Object> result = Map.of("job_id", "job-1", "status", "queued");
        when(storeProfileRepository.findById(7L)).thenReturn(Optional.empty());
        when(client.createAnalysis(file, "1", "A", 20261, 7L, "1")).thenReturn(result);

        assertThat(service.createAnalysis(7L, "1", file, "1", "A", 20261)).isEqualTo(result);
        verify(analysisRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(storeProfileRepository).save(argThat(profile ->
                profile.getUser().getId().equals(7L) && profile.getTrdarCd().equals("1")
                        && profile.getSvcIndutyCd().equals("A")));
    }

    @Test
    void createAnalysisFillsCodesFromSavedProfileWhenOmitted() {
        MockMultipartFile file = new MockMultipartFile("file", "sales.csv", "text/csv", new byte[0]);
        Map<String, Object> result = Map.of("job_id", "job-2", "status", "queued");
        when(storeProfileRepository.findById(7L))
                .thenReturn(Optional.of(AiStoreProfile.create(user, "1", "A")));
        when(client.createAnalysis(file, "1", "A", null, 7L, "1")).thenReturn(result);

        assertThat(service.createAnalysis(7L, "1", file, null, null, null)).isEqualTo(result);
        verify(client).createAnalysis(file, "1", "A", null, 7L, "1");
        verify(storeProfileRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void getAnalysisJobStatusPersistsResultOnceWhenJobCompletes() {
        Map<String, Object> job = Map.of("job_id", "job-3", "status", "completed", "analysis_id", "analysis-3");
        Map<String, Object> analysis = Map.of(
                "analysis_id", "analysis-3", "trdar_cd", "1", "svc_induty_cd", "A",
                "yyqu_cd", 20261, "store_id", "1"
        );
        when(client.getJobStatus("job-3", 7L)).thenReturn(job);
        when(analysisRepository.findByAnalysisIdAndUser_Id("analysis-3", 7L))
                .thenReturn(Optional.empty());
        when(client.getAnalysisResult("analysis-3", 7L)).thenReturn(analysis);

        assertThat(service.getAnalysisJobStatus(7L, "job-3")).isEqualTo(job);

        verify(analysisRepository).save(argThat(saved ->
                saved.getAnalysisId().equals("analysis-3")
                        && saved.getUser().getId().equals(7L)
                        && saved.getStore().getId().equals(1L)
                        && saved.getYyquCd().equals(20261)));
    }

    @Test
    void getAnalysisJobStatusDoesNotRefetchAlreadySavedAnalysis() {
        Map<String, Object> job = Map.of("job_id", "job-4", "status", "completed", "analysis_id", "analysis-4");
        AiAnalysis existing =
                AiAnalysis.create("analysis-4", user, store, "1", "A", 20261, "{}");
        when(client.getJobStatus("job-4", 7L)).thenReturn(job);
        when(analysisRepository.findByAnalysisIdAndUser_Id("analysis-4", 7L))
                .thenReturn(Optional.of(existing));

        assertThat(service.getAnalysisJobStatus(7L, "job-4")).isEqualTo(job);

        verify(client, never()).getAnalysisResult(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(analysisRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void getAnalysisJobStatusSkipsPersistenceWhileStillRunning() {
        Map<String, Object> job = Map.of("job_id", "job-5", "status", "running");
        when(client.getJobStatus("job-5", 7L)).thenReturn(job);

        assertThat(service.getAnalysisJobStatus(7L, "job-5")).isEqualTo(job);

        verify(analysisRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void createAnalysisWithoutCodesOrSavedProfileThrowsNotFoundStore() {
        MockMultipartFile file = new MockMultipartFile("file", "sales.csv", "text/csv", new byte[0]);
        when(storeProfileRepository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createAnalysis(7L, "1", file, null, null, null))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND_STORE);
    }

    @Test
    void recommendationReloadsOwnedAnalysisAndPersistsThread() {
        AiAnalysis analysis = AiAnalysis.create(
                "analysis-1", user, store, "1", "A", 20261,
                "{\"diagnosis\":{\"5_처방\":{\"등급\":\"고객_회복\"}},\"warnings\":[]}"
        );
        when(analysisRepository.findByAnalysisIdAndUser_Id("analysis-1", 7L))
                .thenReturn(Optional.of(analysis));
        when(client.createRecommendation("analysis-1", 7L, "1"))
                .thenReturn(Map.of("thread_id", "thread-1", "상태", "승인 대기"));

        Map<String, Object> result = service.createRecommendation(7L, "analysis-1");

        assertThat(result.get("thread_id")).isEqualTo("thread-1");
        verify(client).createRecommendation("analysis-1", 7L, "1");
        verify(runRepository).save(argThat((AiRecommendationRun run) ->
                run.getThreadId().equals("thread-1")
                        && run.getUser().getId().equals(7L)
                        && run.getAnalysis().getAnalysisId().equals("analysis-1")));
    }

    @Test
    void getRecommendationsReturnsAllRunsWhenNoFilterGiven() {
        AiRecommendationRun run = AiRecommendationRun.create(
                "thread-1",
                AiAnalysis.create("analysis-1", user, store, "1", "A", 20261, "{}"),
                user,
                "{\"상태\":\"승인 대기\"}"
        );
        when(runRepository.findAllByUser_IdOrderByCreatedAtDesc(7L))
                .thenReturn(java.util.List.of(run));

        java.util.List<Map<String, Object>> result = service.getRecommendations(7L);

        assertThat(result).hasSize(1);
    }

    @Test
    void getRecommendationsReturnsOwnedRuns() {
        AiRecommendationRun run = AiRecommendationRun.create(
                "thread-2",
                AiAnalysis.create("analysis-2", user, store, "1", "A", 20261, "{}"),
                user,
                "{\"상태\":\"승인 대기\"}"
        );
        when(runRepository.findAllByUser_IdOrderByCreatedAtDesc(7L))
                .thenReturn(java.util.List.of(run));

        java.util.List<Map<String, Object>> result = service.getRecommendations(7L);

        assertThat(result).hasSize(1);
        verify(runRepository).findAllByUser_IdOrderByCreatedAtDesc(7L);
    }

    @Test
    void getAgentRunHidesHeavyFieldsButPersistsFullResult() {
        AiRecommendationRun run = AiRecommendationRun.create(
                "thread-1",
                AiAnalysis.create("analysis-1", user, store, "1", "A", 20261, "{}"),
                user,
                "{}"
        );
        when(runRepository.findByThreadIdAndUser_Id("thread-1", 7L)).thenReturn(Optional.of(run));
        Map<String, Object> full = Map.of(
                "thread_id", "thread-1",
                "상태", "완료",
                "scm_result", Map.of("counterfactual", "x"),
                "ope_result", Map.of("score", 0.9),
                "대기중_승인", Map.of("방안_후보", java.util.List.of())
        );
        when(client.getAgentRun("thread-1", 7L)).thenReturn(full);

        Map<String, Object> result = service.getAgentRun(7L, "thread-1");

        assertThat(result).doesNotContainKeys("scm_result", "ope_result", "대기중_승인");
        assertThat(result.get("상태")).isEqualTo("완료");
        verify(runRepository).save(argThat((AiRecommendationRun saved) ->
                saved.getResultJson().contains("scm_result")));
    }

    @Test
    void getAgentRunDetailReturnsFullStoredResultIncludingHeavyFields() {
        AiRecommendationRun run = AiRecommendationRun.create(
                "thread-2",
                AiAnalysis.create("analysis-2", user, store, "1", "A", 20261, "{}"),
                user,
                "{\"상태\":\"완료\",\"scm_result\":{\"counterfactual\":\"x\"}}"
        );
        when(runRepository.findByThreadIdAndUser_Id("thread-2", 7L)).thenReturn(Optional.of(run));

        Map<String, Object> result = service.getAgentRunDetail(7L, "thread-2");

        assertThat(result).containsKey("scm_result");
    }

    @Test
    void resumeAgentRunHidesHeavyFieldsButPersistsFullResult() {
        AiRecommendationRun run = AiRecommendationRun.create(
                "thread-3",
                AiAnalysis.create("analysis-3", user, store, "1", "A", 20261, "{}"),
                user,
                "{}"
        );
        when(runRepository.findByThreadIdAndUser_Id("thread-3", 7L)).thenReturn(Optional.of(run));
        Map<String, Object> full = Map.of(
                "thread_id", "thread-3",
                "상태", "완료",
                "shadow_report", Map.of("policy", "shadow")
        );
        when(client.resumeAgentRun(org.mockito.ArgumentMatchers.eq("thread-3"),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(7L)))
                .thenReturn(full);
        AgentRunResumeRequest request =
                new AgentRunResumeRequest(AgentRunResumeRequest.Decision.reject, "사유");

        Map<String, Object> result = service.resumeAgentRun(7L, "thread-3", request);

        assertThat(result).doesNotContainKey("shadow_report");
        verify(runRepository).save(argThat((AiRecommendationRun saved) ->
                saved.getResultJson().contains("shadow_report")));
    }
}
