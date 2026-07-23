package com.bp20.backend.recommendation.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.bp20.backend.recommendation.dto.OrderRecommendationResponse;
import com.bp20.backend.recommendation.dto.AutomaticOrderRecommendationResponse;
import com.bp20.backend.recommendation.service.OrderRecommendationService;
import com.bp20.backend.weather.dto.WeatherResponse;
import com.bp20.backend.weather.service.WeatherService;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/order-recommendations")
public class OrderRecommendationController {

    private final OrderRecommendationService service;
    private final WeatherService weatherService;

    /**
     * 날씨를 고려한 AI 발주 추천을 생성한다.
     *
     * 예:
     * POST /api/order-recommendations/generate
     *      ?latitude=37.5665
     *      &longitude=126.9780
     *
     * 특정 발주시점을 지정하는 경우:
     * POST /api/order-recommendations/generate
     *      ?latitude=37.5665
     *      &longitude=126.9780
     *      &orderDateTime=2026-07-21T15:00:00
     */
    @PostMapping("/generate")
    public List<OrderRecommendationResponse> generate(
            @RequestParam double latitude,
            @RequestParam double longitude,

            @RequestParam(required = false)
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE_TIME
            )
            LocalDateTime orderDateTime
    ) {
        LocalDateTime targetDateTime =
                orderDateTime == null
                        ? LocalDateTime.now()
                        : orderDateTime;

        return service.generate(
                latitude,
                longitude,
                targetDateTime
        );
    }

    /**
     * 브라우저가 확인한 현재 위치로 오늘·내일 날씨와 발주 추천을 한 번에 반환합니다.
     */
    @PostMapping("/automatic")
    public AutomaticOrderRecommendationResponse generateAutomatically(
            @RequestParam double latitude,
            @RequestParam double longitude
    ) {
        LocalDateTime now = LocalDateTime.now();
        List<WeatherResponse> forecasts =
                weatherService.getTodayAndTomorrowWeather(
                        latitude,
                        longitude,
                        now
                );

        List<OrderRecommendationResponse> recommendations =
                service.generate(
                        latitude,
                        longitude,
                        now,
                        forecasts
                );

        return new AutomaticOrderRecommendationResponse(
                latitude,
                longitude,
                forecasts,
                recommendations
        );
    }
}
