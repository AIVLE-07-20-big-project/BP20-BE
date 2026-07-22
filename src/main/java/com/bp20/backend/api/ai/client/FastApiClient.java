package com.bp20.backend.api.ai.client;

import com.bp20.backend.api.ai.dto.request.AgentRunResumeRequest;
import com.bp20.backend.global.exception.FastApiException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;

@Component
public class FastApiClient {

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new ParameterizedTypeReference<>() {};

    private final RestClient restClient;

    public FastApiClient(RestClient fastApiRestClient) {
        this.restClient = fastApiRestClient;
    }

    public Map<String, Object> createAnalysis(
            MultipartFile file, String trdarCd, String svcIndutyCd, Integer yyquCd,
            Long userId, String storeId
    ) {
        return postMultipart("/api/v1/analyses",
                multipartBody(file, trdarCd, svcIndutyCd, yyquCd, userId, storeId));
    }

    private MultiValueMap<String, Object> multipartBody(
            MultipartFile file, String trdarCd, String svcIndutyCd, Integer yyquCd,
            Long userId, String storeId
    ) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("trdar_cd", trdarCd);
        body.add("svc_induty_cd", svcIndutyCd);
        if (yyquCd != null) {
            body.add("yyqu_cd", yyquCd.toString());
        }
        if (userId != null) {
            body.add("user_id", userId.toString());
        }
        if (storeId != null && !storeId.isBlank()) {
            body.add("store_id", storeId);
        }
        body.add("file", toResource(file));
        return body;
    }

    private ByteArrayResource toResource(MultipartFile file) {
        try {
            return new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            };
        } catch (IOException e) {
            throw new UncheckedIOException("Uploaded file could not be read", e);
        }
    }

    public Map<String, Object> createRecommendation(String analysisId) {
        return exchange(() -> restClient.post()
                .uri("/api/v1/analyses/{analysisId}/recommendations", analysisId)
                .retrieve()
                .body(MAP_TYPE));
    }

    public Map<String, Object> getAgentRun(String threadId, Long userId) {
        return exchange(() -> restClient.get()
                .uri("/api/v1/agent-runs/{threadId}", threadId)
                .header("X-User-Id", userId.toString())
                .retrieve()
                .body(MAP_TYPE));
    }

    public Map<String, Object> resumeAgentRun(
            String threadId, AgentRunResumeRequest request, Long userId
    ) {
        return exchange(() -> restClient.post()
                .uri("/api/v1/agent-runs/{threadId}/resume", threadId)
                .header("X-User-Id", userId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(MAP_TYPE));
    }

    private Map<String, Object> postMultipart(String uri, MultiValueMap<String, Object> body) {
        return exchange(() -> restClient.post()
                .uri(uri)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
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
