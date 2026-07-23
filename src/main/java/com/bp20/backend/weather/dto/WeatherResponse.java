package com.bp20.backend.weather.dto;

import java.time.LocalDateTime;

/**
 * 발주시점의 날씨 데이터 응답 DTO입니다.
 *
 * 각 값을 별도 필드로 제공하기 때문에
 * 매출 예측, 재고 추천, 발주 추천 모델에서 바로 사용할 수 있습니다.
 */
public record WeatherResponse(
        LocalDateTime orderDateTime,    // 사용자가 입력한 발주시점
        LocalDateTime forecastDateTime, // 발주시점에 대응되는 기상청 예보시점

        // 요청한 위치
        double latitude,
        double longitude,

        // 기상청 격자 좌표
        int nx,
        int ny,

        Double temperature,         // 기온, 단위: ℃
        Double windSpeed,           // 풍속, 단위: m/s
        String sky,                 // 하늘상태: 맑음, 구름많음, 흐림
        String precipitationType,   // 강수형태: 없음, 비, 비/눈, 눈, 소나기
        Integer rainProbability,    // 강수확률, 단위: %
        Integer humidity            // 습도, 단위: %
) {
}