package com.bp20.backend.api.weather.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
public class WeeklyWeatherClient {

    private final RestClient restClient;

    public WeeklyWeatherClient(@Qualifier("restClientBuilder") RestClient.Builder builder) {
        this.restClient = builder.clone().baseUrl("https://api.open-meteo.com").build();
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getForecast(double latitude, double longitude) {
        Map<String, Object> response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/forecast")
                        .queryParam("latitude", latitude)
                        .queryParam("longitude", longitude)
                        .queryParam("hourly", "temperature_2m,relative_humidity_2m,precipitation_probability,weather_code")
                        .queryParam("forecast_days", 7)
                        .queryParam("timezone", "Asia/Seoul")
                        .build())
                .retrieve()
                .body(Map.class);

        if (response == null || !(response.get("hourly") instanceof Map<?, ?>)) {
            throw new IllegalStateException("일주일 날씨 예보 응답이 올바르지 않습니다.");
        }
        return response;
    }
}
