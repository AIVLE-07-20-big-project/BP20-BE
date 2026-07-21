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
    void createRecommendationUsesSelectedAnalysis() {
        server.expect(requestTo("http://fastapi:8000/api/v1/analyses/analysis-id/recommendations"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {
                          "thread_id": "test-thread-id",
                          "상태": "승인 대기",
                          "대기중_승인": {"방안": "coupon"}
                        }
                        """, MediaType.APPLICATION_JSON));

        Map<String, Object> response = client.createRecommendation("analysis-id");

        assertThat(response.get("thread_id")).isEqualTo("test-thread-id");
        assertThat(response.get("상태")).isEqualTo("승인 대기");
        server.verify();
    }

    @Test
    void getAnalysisCallsFastApiWithAnalysisId() {
        server.expect(requestTo("http://fastapi:8000/api/v1/analyses/analysis-id"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"analysis_id":"analysis-id","report":{"매출":"감소"}}
                        """, MediaType.APPLICATION_JSON));

        Map<String, Object> response = client.getAnalysis("analysis-id");

        assertThat(response.get("analysis_id")).isEqualTo("analysis-id");
        server.verify();
    }

    @Test
    void createAnalysisSendsMultipartFieldsAndFile() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "sample.csv", "text/csv", "a,b\n1,2".getBytes(StandardCharsets.UTF_8)
        );

        server.expect(requestTo("http://fastapi:8000/api/v1/analyses"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(request -> {
                    String contentType = request.getHeaders().getContentType().toString();
                    assertThat(contentType).startsWith(MediaType.MULTIPART_FORM_DATA_VALUE);
                    String body = new String(
                            ((org.springframework.mock.http.client.MockClientHttpRequest) request)
                                    .getBodyAsBytes(),
                            StandardCharsets.UTF_8
                    );
                    assertThat(body).contains("name=\"trdar_cd\"").contains("3110003");
                    assertThat(body).contains("name=\"svc_induty_cd\"").contains("CS100008");
                    assertThat(body).contains("name=\"file\"; filename=\"sample.csv\"");
                    assertThat(body).contains("a,b\n1,2");
                })
                .andRespond(withSuccess("""
                        {"analysis_id":"analysis-id","report":{},"warnings":[]}
                        """, MediaType.APPLICATION_JSON));

        Map<String, Object> response = client.createAnalysis(file, "3110003", "CS100008", null);

        assertThat(response.get("analysis_id")).isEqualTo("analysis-id");
        server.verify();
    }

    @Test
    void resumeAgentRunSendsEnglishDecisionFields() {
        server.expect(requestTo("http://fastapi:8000/api/v1/agent-runs/thread-id/resume"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {"decision":"edit","modificationPlan":"쿠폰발행"}
                        """))
                .andRespond(withSuccess("""
                        {"thread_id":"thread-id","상태":"검증 완료 — 승인 대기"}
                        """, MediaType.APPLICATION_JSON));

        Map<String, Object> response = client.resumeAgentRun(
                "thread-id",
                new AgentRunResumeRequest(AgentRunResumeRequest.Decision.edit, "쿠폰발행")
        );

        assertThat(response.get("thread_id")).isEqualTo("thread-id");
        server.verify();
    }

    @Test
    void propagatesFastApiStatusAndDetail() {
        server.expect(requestTo("http://fastapi:8000/api/v1/agent-runs/missing"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withResourceNotFound()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"detail\":\"agent-run을 찾을 수 없음: missing\"}"));

        assertThatThrownBy(() -> client.getAgentRun("missing"))
                .isInstanceOf(FastApiException.class)
                .satisfies(error -> {
                    FastApiException exception = (FastApiException) error;
                    assertThat(exception.getStatusCode()).isEqualTo(404);
                    assertThat(exception.getMessage()).isEqualTo("agent-run을 찾을 수 없음: missing");
                });

        server.verify();
    }
}
