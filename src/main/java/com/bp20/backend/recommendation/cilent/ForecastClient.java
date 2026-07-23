package com.bp20.backend.forecast;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class ForecastClient {

    private final RestClient restClient;

    public ForecastClient(
            @Qualifier("restClientBuilder")
            RestClient.Builder builder,
            @Value("${ai.server-url}") String aiServerUrl
    ) {
        this.restClient = builder.clone()
                .baseUrl(aiServerUrl)
                .build();
    }

    public ProductForecastResponse predict(
            ForecastRequest request
    ) {
        ProductForecastResponse response = restClient.post()
                .uri("/api/v1/forecasts")
                .body(request)
                .retrieve()
                .body(ProductForecastResponse.class);

        if (response == null) {
            throw new IllegalStateException(
                    "AI 예측 서버에서 결과를 받지 못했습니다."
            );
        }

        return response;
    }
}
