package com.bp20.backend.weather.dto;

/**
 * AI 발주 분석에 사용할 시간별 과거 기상데이터
 */
public record HistoricalWeatherData(

        // 관측 시각: yyyy-MM-dd HH:mm
        String observationTime,

        // 기온: 섭씨
        Double temperature,

        // 풍속: m/s
        Double windSpeed,

        // 하늘 상태: 맑음, 구름많음, 흐림
        String skyCondition,

        // 강수 형태: 없음, 비, 눈, 비/눈
        String precipitationType,

        // 시간 강수량: mm
        Double precipitationAmount,

        // 습도: %
        Double humidity
) {
}