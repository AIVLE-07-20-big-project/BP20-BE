package com.bp20.backend.weather.controller;

import com.bp20.backend.weather.dto.HistoricalWeatherResponse;
import com.bp20.backend.weather.service.HistoricalWeatherService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/weather")
public class HistoricalWeatherController {

    private final HistoricalWeatherService historicalWeatherService;

    public HistoricalWeatherController(
            HistoricalWeatherService historicalWeatherService
    ) {
        this.historicalWeatherService =
                historicalWeatherService;
    }

    /**
     * 위치와 날짜 범위로 과거 시간별 날씨 조회
     *
     * 예시:
     * GET /weather/history
     *     ?latitude=37.5665
     *     &longitude=126.9780
     *     &startDate=2026-07-01
     *     &endDate=2026-07-07
     */
    @GetMapping("/history")
    public ResponseEntity<HistoricalWeatherResponse>
    getHistoricalWeather(
            @RequestParam double latitude,
            @RequestParam double longitude,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate
    ) {
        HistoricalWeatherResponse response =
                historicalWeatherService.getHistoricalWeather(
                        latitude,
                        longitude,
                        startDate,
                        endDate
                );

        return ResponseEntity.ok(response);
    }
}