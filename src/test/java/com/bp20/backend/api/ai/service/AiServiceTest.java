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
    void createAnalysisPersistsFastApiResultAndSavesStoreProfile() {
        MockMultipartFile file = new MockMultipartFile("file", "sales.csv", "text/csv", new byte[0]);
        Map<String, Object> result = Map.of(
                "analysis_id", "analysis-1", "diagnosis", Map.of(), "warnings", java.util.List.of()
        );
        when(storeProfileRepository.findById(7L)).thenReturn(Optional.empty());
        when(client.createAnalysis(file, "1", "A", 20261, 7L, "store-1")).thenReturn(result);

        assertThat(service.createAnalysis(7L, "store-1", file, "1", "A", 20261)).isEqualTo(result);
        verify(analysisRepository).save(argThat(saved ->
                saved.getAnalysisId().equals("analysis-1") && saved.getUserId().equals(7L)
                        && saved.getStoreId().equals("store-1")));
        verify(storeProfileRepository).save(argThat(profile ->
                profile.getUserId().equals(7L) && profile.getTrdarCd().equals("1")
                        && profile.getSvcIndutyCd().equals("A")));
    }

    @Test
    void createAnalysisFillsCodesFromSavedProfileWhenOmitted() {
        MockMultipartFile file = new MockMultipartFile("file", "sales.csv", "text/csv", new byte[0]);
        Map<String, Object> result = Map.of(
                "analysis_id", "analysis-2", "diagnosis", Map.of(), "warnings", java.util.List.of()
        );
        when(storeProfileRepository.findById(7L))
                .thenReturn(Optional.of(AiStoreProfile.create(7L, "1", "A")));
        when(client.createAnalysis(file, "1", "A", null, 7L, "store-1")).thenReturn(result);

        assertThat(service.createAnalysis(7L, "store-1", file, null, null, null)).isEqualTo(result);
        verify(client).createAnalysis(file, "1", "A", null, 7L, "store-1");
        verify(storeProfileRepository, never()).save(org.mockito.ArgumentMatchers.any());
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
