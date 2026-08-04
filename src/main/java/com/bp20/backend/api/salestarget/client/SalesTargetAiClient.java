package com.bp20.backend.api.salestarget.client;

import com.bp20.backend.global.exception.FastApiException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

/**
 * AI 서버(app/sales_target/router.py)의 LangGraph 배치 실행 API를 호출한다.
 * api/ai/client/FastApiClient와 동일한 구조(같은 fastApiRestClient 빈 재사용, 응답은 타입을
 * 강제하지 않고 Map<String,Object>로 그대로 통과시킴)를 그대로 따른다 — AI가 돌려주는
 * "상태"/"대기중_승인"/"후보_리스트" 같은 한글 키를 BE가 다시 감쌀 필요 없이 FE까지 그대로
 * 내려주기 위함(AI_Agent_전환_가이드라인.md 3단계).
 *
 * sales-target 그래프는 app/main.py에 같은 FastAPI 앱으로 얹혀 있어서 별도 base-url 설정 없이
 * FastApiClient가 쓰는 fastApiRestClient 빈을 그대로 재사용한다.
 */
@Component
public class SalesTargetAiClient {

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new ParameterizedTypeReference<>() {};

    private final RestClient restClient;

    public SalesTargetAiClient(RestClient fastApiRestClient) {
        this.restClient = fastApiRestClient;
    }

    public Map<String, Object> startBatch(Integer topN) {
        return exchange(() -> restClient.post()
                .uri("/api/v1/sales-targets/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("top_n", topN != null ? topN : 20))
                .retrieve()
                .body(MAP_TYPE));
    }

    public Map<String, Object> getBatch(String threadId) {
        return exchange(() -> restClient.get()
                .uri("/api/v1/sales-targets/jobs/{threadId}", threadId)
                .retrieve()
                .body(MAP_TYPE));
    }

    public Map<String, Object> approveBatch(String threadId) {
        return exchange(() -> restClient.post()
                .uri("/api/v1/sales-targets/jobs/{threadId}/approve", threadId)
                .retrieve()
                .body(MAP_TYPE));
    }

    public Map<String, Object> rejectBatch(String threadId) {
        return exchange(() -> restClient.post()
                .uri("/api/v1/sales-targets/jobs/{threadId}/reject", threadId)
                .retrieve()
                .body(MAP_TYPE));
    }

    private Map<String, Object> exchange(Request request) {
        try {
            return request.execute();
        } catch (RestClientResponseException e) {
            throw new FastApiException(e.getStatusCode().value(), extractMessage(e));
        }
    }

    private String extractMessage(RestClientResponseException e) {
        try {
            Map<String, Object> body = e.getResponseBodyAs(MAP_TYPE);
            Object detail = body == null ? null : body.get("detail");
            return detail == null ? "AI service request failed" : detail.toString();
        } catch (Exception ignored) {
            return "AI service request failed";
        }
    }

    @FunctionalInterface
    private interface Request {
        Map<String, Object> execute();
    }
}
