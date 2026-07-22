package com.bp20.backend.api.ai.client;

import com.bp20.backend.api.ai.dto.request.AgentRunResumeRequest;
import com.bp20.backend.global.exception.FastApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class FastApiClientTest {

    private MockRestServiceServer server;
    private FastApiClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://fastapi:8000");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new FastApiClient(builder.build());
    }

    @Test
    void createRecommendationUsesAnalysisId() {
        server.expect(requestTo("http://fastapi:8000/api/v1/analyses/analysis-id/recommendations"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-User-Id", "7"))
                .andRespond(withSuccess("""
                        {"thread_id":"test-thread-id","상태":"승인 대기"}
                        """, MediaType.APPLICATION_JSON));

        Map<String, Object> response = client.createRecommendation("analysis-id", 7L);

        assertThat(response.get("thread_id")).isEqualTo("test-thread-id");
        server.verify();
    }

    @Test
    void createAnalysisSendsUserAndStoreOwnership() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "sample.csv", "text/csv", "a,b\n1,2".getBytes(StandardCharsets.UTF_8)
        );
        server.expect(requestTo("http://fastapi:8000/api/v1/analyses"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(request -> {
                    String body = new String(
                            ((org.springframework.mock.http.client.MockClientHttpRequest) request)
                                    .getBodyAsBytes(), StandardCharsets.UTF_8
                    );
                    assertThat(body).contains("name=\"user_id\"").contains("7");
                    assertThat(body).contains("name=\"store_id\"").contains("store-1");
                })
                .andRespond(withSuccess("""
                        {"analysis_id":"analysis-id","report":{},"diagnosis":{},"warnings":[]}
                        """, MediaType.APPLICATION_JSON));

        client.createAnalysis(file, "3110003", "CS100008", 20261, 7L, "store-1");
        server.verify();
    }

    @Test
    void resumeAgentRunSendsEnglishDecisionFields() {
        server.expect(requestTo("http://fastapi:8000/api/v1/agent-runs/thread-id/resume"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-User-Id", "7"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {"decision":"edit","modificationPlan":"쿠폰발행"}
                        """))
                .andRespond(withSuccess("""
                        {"thread_id":"thread-id","상태":"검증 완료 — 승인 대기"}
                        """, MediaType.APPLICATION_JSON));

        Map<String, Object> response = client.resumeAgentRun(
                "thread-id",
                new AgentRunResumeRequest(AgentRunResumeRequest.Decision.edit, "쿠폰발행"),
                7L
        );

        assertThat(response.get("thread_id")).isEqualTo("thread-id");
        server.verify();
    }

    @Test
    void propagatesFastApiStatusAndDetail() {
        server.expect(requestTo("http://fastapi:8000/api/v1/agent-runs/missing"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-User-Id", "7"))
                .andRespond(withResourceNotFound()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"detail\":\"agent-run을 찾을 수 없음: missing\"}"));

        assertThatThrownBy(() -> client.getAgentRun("missing", 7L))
                .isInstanceOf(FastApiException.class)
                .satisfies(error -> {
                    FastApiException exception = (FastApiException) error;
                    assertThat(exception.getStatusCode()).isEqualTo(404);
                    assertThat(exception.getMessage()).isEqualTo("agent-run을 찾을 수 없음: missing");
                });

        server.verify();
    }
}
