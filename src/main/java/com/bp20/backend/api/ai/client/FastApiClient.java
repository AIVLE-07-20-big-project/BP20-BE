package com.bp20.backend.api.ai.client;

import com.bp20.backend.api.ai.dto.request.AgentRunRequest;
import com.bp20.backend.api.ai.dto.request.AgentRunResumeRequest;
import com.bp20.backend.api.ai.dto.request.CampaignLogRequest;
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

    public Map<String, Object> createReport(
            MultipartFile file, String trdarCd, String svcIndutyCd, Integer yyquCd
    ) {
        return postMultipart("/api/v1/reports", multipartBody(file, trdarCd, svcIndutyCd, yyquCd));
    }

    public Map<String, Object> createAnalysis(
            MultipartFile file, String trdarCd, String svcIndutyCd, Integer yyquCd
    ) {
        return postMultipart("/api/v1/analyses", multipartBody(file, trdarCd, svcIndutyCd, yyquCd));
    }

    private MultiValueMap<String, Object> multipartBody(
            MultipartFile file, String trdarCd, String svcIndutyCd, Integer yyquCd
    ) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("trdar_cd", trdarCd);
        body.add("svc_induty_cd", svcIndutyCd);
        if (yyquCd != null) {
            body.add("yyqu_cd", yyquCd.toString());
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

    public Map<String, Object> getAnalysis(String analysisId) {
        return exchange(() -> restClient.get()
                .uri("/api/v1/analyses/{analysisId}", analysisId)
                .retrieve()
                .body(MAP_TYPE));
    }

    public Map<String, Object> createAnalysisReport(String analysisId) {
        return exchange(() -> restClient.post()
                .uri("/api/v1/analyses/{analysisId}/reports", analysisId)
                .retrieve()
                .body(MAP_TYPE));
    }

    public Map<String, Object> createAgentRun(AgentRunRequest request) {
        return postJson("/api/v1/agent-runs", request);
    }

    public Map<String, Object> getAgentRun(String threadId) {
        return exchange(() -> restClient.get()
                .uri("/api/v1/agent-runs/{threadId}", threadId)
                .retrieve()
                .body(MAP_TYPE));
    }

    public Map<String, Object> resumeAgentRun(String threadId, AgentRunResumeRequest request) {
        return postJson("/api/v1/agent-runs/{threadId}/resume", request, threadId);
    }

    public Map<String, Object> createCampaignLog(CampaignLogRequest request) {
        return postJson("/api/v1/campaign-logs", request);
    }

    public Map<String, Object> getCampaignLogQuality() {
        return exchange(() -> restClient.get()
                .uri("/api/v1/campaign-logs/quality")
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

    private Map<String, Object> postJson(String uri, Object body, Object... uriVariables) {
        return exchange(() -> restClient.post()
                .uri(uri, uriVariables)
                .contentType(MediaType.APPLICATION_JSON)
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
