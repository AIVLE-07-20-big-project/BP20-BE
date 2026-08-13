package com.bp20.backend.api.effectverification.client;

import com.bp20.backend.api.effectverification.dto.request.EffectVerificationFromAnalysesRequest;
import com.bp20.backend.api.effectverification.dto.request.EffectVerificationRequest;
import com.bp20.backend.api.effectverification.dto.response.EffectVerificationFromAnalysesResponse;
import com.bp20.backend.api.effectverification.dto.response.EffectVerificationResponse;
import com.bp20.backend.api.effectverification.exception.EffectVerificationAiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class EffectVerificationApiClient {

    private final RestClient restClient;

    public EffectVerificationApiClient(
            RestClient.Builder externalRestClientBuilder,
            @Value("${ai.effect-verification.base-url}") String baseUrl
    ) {
        this.restClient = externalRestClientBuilder.clone()
                .baseUrl(baseUrl)
                .build();
    }

    public EffectVerificationResponse verifyEffect(
            EffectVerificationRequest request
    ) {
        try {
            EffectVerificationResponse response = restClient.post()
                    .uri("/effect-verification/verify")
                    .body(request)
                    .retrieve()
                    .body(EffectVerificationResponse.class);

            if (response == null) {
                throw new EffectVerificationAiException(
                        "효과 검증 AI 서버가 빈 응답을 반환했습니다."
                );
            }
            return response;
        } catch (RestClientException exception) {
            throw new EffectVerificationAiException(
                    "효과 검증 AI 호출에 실패했습니다.",
                    exception
            );
        }
    }

    public EffectVerificationFromAnalysesResponse verifyEffectFromAnalyses(
            EffectVerificationFromAnalysesRequest request
    ) {
        try {
            EffectVerificationFromAnalysesResponse response = restClient.post()
                    .uri("/effect-verification/verify-from-analyses")
                    .body(request)
                    .retrieve()
                    .body(EffectVerificationFromAnalysesResponse.class);

            if (response == null) {
                throw new EffectVerificationAiException(
                        "효과 검증 AI 서버가 빈 응답을 반환했습니다."
                );
            }
            return response;
        } catch (RestClientException exception) {
            throw new EffectVerificationAiException(
                    "분석 결과 기반 효과 검증 AI 호출에 실패했습니다.",
                    exception
            );
        }
    }
}
