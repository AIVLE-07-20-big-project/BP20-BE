package com.bp20.backend.api.ai.service;

import com.bp20.backend.api.ai.client.FastApiClient;
import com.bp20.backend.api.ai.domain.AiAnalysis;
import com.bp20.backend.api.ai.domain.AiRecommendationRun;
import com.bp20.backend.api.ai.domain.AiStoreProfile;
import com.bp20.backend.api.ai.repository.AiAnalysisRepository;
import com.bp20.backend.api.ai.repository.AiRecommendationRunRepository;
import com.bp20.backend.api.ai.repository.AiStoreProfileRepository;
import com.bp20.backend.global.exception.ApiException;
import com.bp20.backend.global.response.ErrorCode;
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
    private AiService service;

    @BeforeEach
    void setUp() {
        client = mock(FastApiClient.class);
        analysisRepository = mock(AiAnalysisRepository.class);
        runRepository = mock(AiRecommendationRunRepository.class);
        storeProfileRepository = mock(AiStoreProfileRepository.class);
        service = new AiService(client, analysisRepository, runRepository, storeProfileRepository, JsonMapper.builder().build());
    }

    @Test
    void createAnalysisReturnsJobAndSavesStoreProfileWithoutPersistingAnalysisYet() {
        MockMultipartFile file = new MockMultipartFile("file", "sales.csv", "text/csv", new byte[0]);
        Map<String, Object> result = Map.of("job_id", "job-1", "status", "queued");
        when(storeProfileRepository.findById(7L)).thenReturn(Optional.empty());
        when(client.createAnalysis(file, "1", "A", 20261, 7L, "store-1")).thenReturn(result);

        assertThat(service.createAnalysis(7L, "store-1", file, "1", "A", 20261)).isEqualTo(result);
        verify(analysisRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(storeProfileRepository).save(argThat(profile ->
                profile.getUserId().equals(7L) && profile.getTrdarCd().equals("1")
                        && profile.getSvcIndutyCd().equals("A")));
    }

    @Test
    void createAnalysisFillsCodesFromSavedProfileWhenOmitted() {
        MockMultipartFile file = new MockMultipartFile("file", "sales.csv", "text/csv", new byte[0]);
        Map<String, Object> result = Map.of("job_id", "job-2", "status", "queued");
        when(storeProfileRepository.findById(7L))
                .thenReturn(Optional.of(AiStoreProfile.create(7L, "1", "A")));
        when(client.createAnalysis(file, "1", "A", null, 7L, "store-1")).thenReturn(result);

        assertThat(service.createAnalysis(7L, "store-1", file, null, null, null)).isEqualTo(result);
        verify(client).createAnalysis(file, "1", "A", null, 7L, "store-1");
        verify(storeProfileRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void getAnalysisJobStatusPersistsResultOnceWhenJobCompletes() {
        Map<String, Object> job = Map.of("job_id", "job-3", "status", "completed", "analysis_id", "analysis-3");
        Map<String, Object> analysis = Map.of(
                "analysis_id", "analysis-3", "trdar_cd", "1", "svc_induty_cd", "A",
                "yyqu_cd", 20261, "store_id", "store-1"
        );
        when(client.getJobStatus("job-3", 7L)).thenReturn(job);
        when(analysisRepository.findByAnalysisIdAndUserId("analysis-3", 7L)).thenReturn(Optional.empty());
        when(client.getAnalysisResult("analysis-3", 7L)).thenReturn(analysis);

        assertThat(service.getAnalysisJobStatus(7L, "job-3")).isEqualTo(job);

        verify(analysisRepository).save(argThat(saved ->
                saved.getAnalysisId().equals("analysis-3") && saved.getUserId().equals(7L)
                        && saved.getStoreId().equals("store-1") && saved.getYyquCd().equals(20261)));
    }

    @Test
    void getAnalysisJobStatusDoesNotRefetchAlreadySavedAnalysis() {
        Map<String, Object> job = Map.of("job_id", "job-4", "status", "completed", "analysis_id", "analysis-4");
        AiAnalysis existing = AiAnalysis.create("analysis-4", 7L, "store-1", "1", "A", 20261, "{}");
        when(client.getJobStatus("job-4", 7L)).thenReturn(job);
        when(analysisRepository.findByAnalysisIdAndUserId("analysis-4", 7L)).thenReturn(Optional.of(existing));

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

        assertThatThrownBy(() -> service.createAnalysis(7L, "store-1", file, null, null, null))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND_STORE);
    }

    @Test
    void recommendationReloadsOwnedAnalysisAndPersistsThread() {
        AiAnalysis analysis = AiAnalysis.create(
                "analysis-1", 7L, "store-1", "1", "A", 20261,
                "{\"diagnosis\":{\"5_처방\":{\"등급\":\"고객_회복\"}},\"warnings\":[]}"
        );
        when(analysisRepository.findByAnalysisIdAndUserId("analysis-1", 7L))
                .thenReturn(Optional.of(analysis));
        when(client.createRecommendation("analysis-1", 7L))
                .thenReturn(Map.of("thread_id", "thread-1", "상태", "승인 대기"));

        Map<String, Object> result = service.createRecommendation(7L, "analysis-1");

        assertThat(result.get("thread_id")).isEqualTo("thread-1");
        verify(client).createRecommendation("analysis-1", 7L);
        verify(runRepository).save(argThat((AiRecommendationRun run) ->
                run.getThreadId().equals("thread-1") && run.getUserId().equals(7L)
                        && run.getStoreId().equals("store-1")));
    }

    @Test
    void getRecommendationsReturnsAllRunsWithStoreIdWhenNoFilterGiven() {
        AiRecommendationRun run = AiRecommendationRun.create(
                "thread-1", "analysis-1", 7L, "store-1", "{\"상태\":\"승인 대기\"}"
        );
        when(runRepository.findAllByUserIdOrderByCreatedAtDesc(7L)).thenReturn(java.util.List.of(run));

        java.util.List<Map<String, Object>> result = service.getRecommendations(7L, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).get("store_id")).isEqualTo("store-1");
    }

    @Test
    void getRecommendationsFiltersByStoreIdWhenGiven() {
        AiRecommendationRun run = AiRecommendationRun.create(
                "thread-2", "analysis-2", 7L, "store-2", "{\"상태\":\"승인 대기\"}"
        );
        when(runRepository.findAllByUserIdAndStoreIdOrderByCreatedAtDesc(7L, "store-2"))
                .thenReturn(java.util.List.of(run));

        java.util.List<Map<String, Object>> result = service.getRecommendations(7L, "store-2");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).get("store_id")).isEqualTo("store-2");
        verify(runRepository, never()).findAllByUserIdOrderByCreatedAtDesc(org.mockito.ArgumentMatchers.any());
    }
}
