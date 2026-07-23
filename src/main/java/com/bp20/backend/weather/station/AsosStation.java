package com.bp20.backend.weather.station;

/**
 * ASOS 관측소 정보
 */
public record AsosStation(
        String stationId,
        String stationName,
        double latitude,
        double longitude
) {
}