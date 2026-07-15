package com.bp20.backend.effectverification.client;

import com.bp20.backend.effectverification.dto.request.EffectVerificationRequest;
import com.bp20.backend.effectverification.dto.response.EffectVerificationResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class EffectVerificationApiClient {

    private final RestClient restClient;

    public EffectVerificationApiClient(
            @Value("${ai.effect-verification.base-url}") String baseUrl
    ) {

        this.restClient = RestClient.builder()
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
                throw new IllegalStateException(
                        "AI 서버가 빈 응답을 반환했습니다."
                );
            }

            return response;

        } catch (RestClientException e) {

            throw new IllegalStateException(
                    "Effect Verification AI 호출 실패",
                    e
            );
        }
    }
}
