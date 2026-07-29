package com.bp20.backend.api.recommendation.dto;

import com.bp20.backend.api.weather.dto.DailyWeatherResponse;

import java.util.List;

/**
 * 브라우저에서 확인한 현재 위치를 기준으로 실행한 발주 추천 결과입니다.
 */
public record AutomaticOrderRecommendationResponse(
        double latitude,
        double longitude,
        List<DailyWeatherResponse> weatherForecasts,
        List<OrderRecommendationResponse> recommendations
) {
}
