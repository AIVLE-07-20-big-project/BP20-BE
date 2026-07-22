package com.bp20.backend.api.ai.service;

import com.bp20.backend.api.ai.client.FastApiClient;
import com.bp20.backend.api.ai.domain.AiAnalysis;
import com.bp20.backend.api.ai.domain.AiRecommendationRun;
import com.bp20.backend.api.ai.repository.AiAnalysisRepository;
import com.bp20.backend.api.ai.repository.AiRecommendationRunRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiServiceTest {
    private FastApiClient client;
    private AiAnalysisRepository analysisRepository;
    private AiRecommendationRunRepository runRepository;
    private AiService service;

    @BeforeEach
    void setUp() {
        client = mock(FastApiClient.class);
        analysisRepository = mock(AiAnalysisRepository.class);
        runRepository = mock(AiRecommendationRunRepository.class);
        service = new AiService(client, analysisRepository, runRepository, JsonMapper.builder().build());
    }

    @Test
    void createAnalysisPersistsFastApiResult() {
        MockMultipartFile file = new MockMultipartFile("file", "sales.csv", "text/csv", new byte[0]);
        Map<String, Object> result = Map.of(
                "analysis_id", "analysis-1", "diagnosis", Map.of(), "warnings", java.util.List.of()
        );
        when(client.createAnalysis(file, "1", "A", 20261, 7L, "store-1")).thenReturn(result);

        assertThat(service.createAnalysis(7L, "store-1", file, "1", "A", 20261)).isEqualTo(result);
        verify(analysisRepository).save(argThat(saved ->
                saved.getAnalysisId().equals("analysis-1") && saved.getUserId().equals(7L)
                        && saved.getStoreId().equals("store-1")));
    }

    @Test
    void recommendationReloadsOwnedAnalysisAndPersistsThread() {
        AiAnalysis analysis = AiAnalysis.create(
                "analysis-1", 7L, "store-1", "1", "A", 20261,
                "{\"diagnosis\":{\"5_처방\":{\"등급\":\"고객_회복\"}},\"warnings\":[]}"
        );
        when(analysisRepository.findByAnalysisIdAndUserId("analysis-1", 7L))
                .thenReturn(Optional.of(analysis));
        when(client.createRecommendation("analysis-1"))
                .thenReturn(Map.of("thread_id", "thread-1", "상태", "승인 대기"));

        Map<String, Object> result = service.createRecommendation(7L, "analysis-1");

        assertThat(result.get("thread_id")).isEqualTo("thread-1");
        verify(client).createRecommendation("analysis-1");
        verify(runRepository).save(argThat((AiRecommendationRun run) ->
                run.getThreadId().equals("thread-1") && run.getUserId().equals(7L)));
    }
}
