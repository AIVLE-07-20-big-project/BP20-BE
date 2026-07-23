package com.bp20.backend.recommendation.dto;

import com.bp20.backend.weather.dto.WeatherResponse;

import java.util.List;

/**
 * 브라우저에서 확인한 현재 위치를 기준으로 실행한 발주 추천 결과입니다.
 */
public record AutomaticOrderRecommendationResponse(
        double latitude,
        double longitude,
        List<WeatherResponse> weatherForecasts,
        List<OrderRecommendationResponse> recommendations
) {
}
