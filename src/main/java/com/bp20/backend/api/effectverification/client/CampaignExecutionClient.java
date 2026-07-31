package com.bp20.backend.api.effectverification.client;

import com.bp20.backend.global.exception.FastApiException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

@Component
public class CampaignExecutionClient {

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new ParameterizedTypeReference<>() {};

    private final RestClient restClient;

    public CampaignExecutionClient(RestClient fastApiRestClient) {
        this.restClient = fastApiRestClient;
    }

    public Map<String, Object> recordExecution(
            String threadId,
            Long userId,
            int treatmentYyquCd
    ) {
        try {
            return restClient.post()
                    .uri("/api/v1/campaign-logs")
                    .header("X-User-Id", userId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "thread_id", threadId,
                            "executed", true,
                            "treatment_yyqu_cd", treatmentYyquCd
                    ))
                    .retrieve()
                    .body(MAP_TYPE);
        } catch (RestClientResponseException exception) {
            throw new FastApiException(
                    exception.getStatusCode().value(),
                    extractMessage(exception)
            );
        }
    }

    private String extractMessage(RestClientResponseException exception) {
        try {
            Map<String, Object> body = exception.getResponseBodyAs(MAP_TYPE);
            Object detail = body == null ? null : body.get("detail");
            return detail == null
                    ? "Campaign execution request failed"
                    : detail.toString();
        } catch (Exception ignored) {
            return "Campaign execution request failed";
        }
    }
}
