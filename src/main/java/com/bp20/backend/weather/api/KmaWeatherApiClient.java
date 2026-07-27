package com.bp20.backend.weather.api;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.URI;
import java.util.Map;

@Component
public class KmaWeatherApiClient {

    private final RestClient restClient;

    public KmaWeatherApiClient(
            @Qualifier("restClientBuilder") RestClient.Builder builder
    ) {
        this.restClient = builder.clone().build();
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getForecast(URI requestUri) {
        try {
            return restClient.get()
                    .uri(requestUri)
                    .retrieve()
                    .body(Map.class);
        } catch (RestClientException exception) {
            throw new IllegalStateException("기상청 단기예보 API 호출에 실패했습니다.", exception);
        }
    }

    public JsonNode getHistoricalWeather(URI requestUri) {
        JsonNode response = restClient.get()
                .uri(requestUri)
                .retrieve()
                .onStatus(
                        HttpStatusCode::isError,
                        (request, errorResponse) -> {
                            throw new IllegalStateException(
                                    "기상청 과거 날씨 API HTTP 오류: "
                                            + errorResponse.getStatusCode()
                            );
                        }
                )
                .body(JsonNode.class);

        if (response == null) {
            throw new IllegalStateException("기상청 과거 날씨 API 응답이 비어 있습니다.");
        }
        return response;
    }
}
