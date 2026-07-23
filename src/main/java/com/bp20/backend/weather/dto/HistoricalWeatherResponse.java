package com.bp20.backend.weather.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * 특정 위치의 과거 기상데이터 전체 응답 DTO
 */
public record HistoricalWeatherResponse(

        // 선택된 ASOS 관측소 번호
        String stationId,

        // 선택된 ASOS 관측소 이름
        String stationName,

        // 사용자가 전달한 위도
        double latitude,

        // 사용자가 전달한 경도
        double longitude,

        // 조회 시작일
        LocalDate startDate,

        // 조회 종료일
        LocalDate endDate,

        // 시간별 기상데이터
        List<HistoricalWeatherData> weathers
) {
}