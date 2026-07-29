package com.bp20.backend.api.weather.dto;

import java.time.LocalDate;

public record DailyWeatherResponse(
        LocalDate date,
        double latitude,
        double longitude,
        Double maximumTemperature,
        Double minimumTemperature,
        String weatherCondition,
        Integer rainProbability,
        Integer humidity
) {
}
