package com.bp20.backend.weather.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bp20.backend.weather.dto.WeatherResponse;
import com.bp20.backend.weather.service.WeatherService;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/weather")
@RequiredArgsConstructor
public class WeatherController {

    private final WeatherService weatherService;

    /**
     * 지정한 발주시점의 날씨를 조회합니다.
     *
     * 요청 예시:
     * GET /api/weather/order
     * ?latitude=37.5665
     * &longitude=126.9780
     * &orderDateTime=2026-07-20T15:00:00
     */
    @GetMapping("/order")
    public WeatherResponse getOrderWeather(
            @RequestParam double latitude,
            @RequestParam double longitude,

            @RequestParam
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE_TIME
            )
            LocalDateTime orderDateTime
    ) {
        return weatherService.getOrderWeather(
                latitude,
                longitude,
                orderDateTime
        );
    }

    /**
     * 현재 시점을 발주시점으로 사용하는 API
     * GET /api/weather/order/now
     * ?latitude=37.5665
     * &longitude=126.9780
     */
    @GetMapping("/order/now")
    public WeatherResponse getCurrentOrderWeather(
            @RequestParam double latitude,
            @RequestParam double longitude
    ) {
        return weatherService.getOrderWeather(
                latitude,
                longitude,
                LocalDateTime.now()
        );
    }


    // 위도·경도에서 기상청 격자 좌표를 확인하는 개발 및 테스트용 API
    @GetMapping("/grid")
    public WeatherService.Grid getGrid(
            @RequestParam double latitude,
            @RequestParam double longitude
    ) {
        return weatherService.getGrid(
                latitude,
                longitude
        );
    }
}