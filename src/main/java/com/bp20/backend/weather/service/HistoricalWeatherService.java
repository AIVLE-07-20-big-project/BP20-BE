package com.bp20.backend.weather.service;

import com.bp20.backend.weather.dto.HistoricalWeatherData;
import com.bp20.backend.weather.dto.HistoricalWeatherResponse;
import com.bp20.backend.weather.station.AsosStation;
import com.bp20.backend.weather.station.AsosStationResolver;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Service
public class HistoricalWeatherService {

    private static final ZoneId KOREA_ZONE =
            ZoneId.of("Asia/Seoul");

    private static final DateTimeFormatter REQUEST_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd");

    private final RestClient restClient;
    private final AsosStationResolver stationResolver;
    private final String serviceKey;

    public HistoricalWeatherService(
            RestClient.Builder restClientBuilder,
            AsosStationResolver stationResolver,
            @Value("${weather.api.service-key}") String serviceKey,
            @Value("${weather.api.asos-hourly-url}") String asosHourlyUrl
    ) {
        this.restClient = restClientBuilder
                .baseUrl(asosHourlyUrl)
                .build();

        this.stationResolver = stationResolver;
        this.serviceKey = serviceKey;
    }

    /**
     * 특정 좌표와 기간에 대한 시간별 과거 기상데이터 조회
     */
    public HistoricalWeatherResponse getHistoricalWeather(
            double latitude,
            double longitude,
            LocalDate startDate,
            LocalDate endDate
    ) {
        validateDates(startDate, endDate);

        // 사용자 위치에서 가장 가까운 ASOS 관측소 선택
        AsosStation station =
                stationResolver.findNearestStation(latitude, longitude);

        List<HistoricalWeatherData> weatherData =
                requestAllPages(station.stationId(), startDate, endDate);

        weatherData.sort(
                Comparator.comparing(
                        HistoricalWeatherData::observationTime
                )
        );

        return new HistoricalWeatherResponse(
                station.stationId(),
                station.stationName(),
                latitude,
                longitude,
                startDate,
                endDate,
                weatherData
        );
    }

    /**
     * 기상청 API의 전체 페이지를 반복 조회합니다.
     *
     * 하루 24건이므로 조회 기간에 맞춰 numOfRows를 계산하지만,
     * API 결과 수가 제한될 수 있어 페이지 처리를 함께 수행합니다.
     */
    private List<HistoricalWeatherData> requestAllPages(
            String stationId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        List<HistoricalWeatherData> result = new ArrayList<>();

        int pageNo = 1;
        int numOfRows = 999;

        while (true) {
            JsonNode root = requestPage(
                    stationId,
                    startDate,
                    endDate,
                    pageNo,
                    numOfRows
            );

            JsonNode body = root.path("response").path("body");

            int totalCount = body.path("totalCount").asInt(0);

            JsonNode itemsNode =
                    body.path("items").path("item");

            result.addAll(parseItems(itemsNode));

            if (totalCount == 0
                    || pageNo * numOfRows >= totalCount) {
                break;
            }

            pageNo++;
        }

        return result;
    }

    /**
     * ASOS 시간자료 API 한 페이지 호출
     */
    private JsonNode requestPage(
            String stationId,
            LocalDate startDate,
            LocalDate endDate,
            int pageNo,
            int numOfRows
    ) {
        JsonNode response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .queryParam("serviceKey", serviceKey)
                        .queryParam("pageNo", pageNo)
                        .queryParam("numOfRows", numOfRows)
                        .queryParam("dataType", "JSON")
                        .queryParam("dataCd", "ASOS")
                        .queryParam("dateCd", "HR")
                        .queryParam(
                                "startDt",
                                startDate.format(REQUEST_DATE_FORMAT)
                        )
                        .queryParam("startHh", "00")
                        .queryParam(
                                "endDt",
                                endDate.format(REQUEST_DATE_FORMAT)
                        )
                        .queryParam("endHh", "23")
                        .queryParam("stnIds", stationId)
                        .build())
                .retrieve()
                .onStatus(
                        HttpStatusCode::isError,
                        (request, errorResponse) -> {
                            throw new IllegalStateException(
                                    "기상청 API HTTP 오류: "
                                            + errorResponse.getStatusCode()
                            );
                        }
                )
                .body(JsonNode.class);

        if (response == null) {
            throw new IllegalStateException(
                    "기상청 API 응답이 비어 있습니다."
            );
        }

        validateApiResponse(response);

        return response;
    }

    /**
     * 공공데이터포털 API의 resultCode 검사
     */
    private void validateApiResponse(JsonNode root) {
        JsonNode responseNode = root.path("response");
        JsonNode header = responseNode.path("header");

        String resultCode = header.path("resultCode").asText();
        String resultMessage = header.path("resultMsg").asText();

        if (resultCode.isBlank()) {
            throw new IllegalStateException(
                    "기상청 응답 형식이 올바르지 않습니다. 응답 내용: "
                            + root
            );
        }

        if (!"00".equals(resultCode)) {
            throw new IllegalStateException(
                    "기상청 API 오류: resultCode="
                            + resultCode
                            + ", resultMsg="
                            + resultMessage
            );
        }
    }

    /**
     * item 배열을 서비스에서 사용할 DTO로 변환
     */
    private List<HistoricalWeatherData> parseItems(
            JsonNode itemsNode
    ) {
        if (itemsNode == null
                || itemsNode.isMissingNode()
                || itemsNode.isNull()) {
            return Collections.emptyList();
        }

        List<HistoricalWeatherData> weatherList =
                new ArrayList<>();

        if (itemsNode.isArray()) {
            for (JsonNode item : itemsNode) {
                weatherList.add(convertToWeatherData(item));
            }
        } else if (itemsNode.isObject()) {
            // 결과가 한 건일 때 객체 형태로 반환될 가능성 처리
            weatherList.add(convertToWeatherData(itemsNode));
        }

        return weatherList;
    }

    /**
     * 기상청 응답 필드를 AI 분석용 날씨 DTO로 변환
     */
    private HistoricalWeatherData convertToWeatherData(
            JsonNode item
    ) {
        Double temperature =
                getNullableDouble(item, "ta");

        Double windSpeed =
                getNullableDouble(item, "ws");

        Double humidity =
                getNullableDouble(item, "hm");

        Double precipitationAmount =
                getNullableDouble(item, "rn");

        Double snowDepth =
                getNullableDouble(item, "dsnw");

        Double newSnowDepth =
                getNullableDouble(item, "hr3Fhsc");

        Double totalCloudAmount =
                getNullableDouble(item, "dc10Tca");

        return new HistoricalWeatherData(
                item.path("tm").asText(null),
                temperature,
                windSpeed,
                convertSkyCondition(totalCloudAmount),
                convertPrecipitationType(
                        precipitationAmount,
                        snowDepth,
                        newSnowDepth
                ),
                precipitationAmount == null
                        ? 0.0
                        : precipitationAmount,
                humidity
        );
    }

    /**
     * 전운량을 단기예보의 하늘 상태와 비슷한 형태로 변환합니다.
     *
     * ASOS 전운량 범위는 일반적으로 0~10입니다.
     */
    private String convertSkyCondition(
            Double totalCloudAmount
    ) {
        if (totalCloudAmount == null) {
            return "정보없음";
        }

        if (totalCloudAmount <= 2.0) {
            return "맑음";
        }

        if (totalCloudAmount <= 7.0) {
            return "구름많음";
        }

        return "흐림";
    }

    /**
     * 실제 관측 강수량과 적설량으로 강수 형태를 추정합니다.
     *
     * ASOS 시간자료가 모든 시점에서 예보 API의 PTY와 동일한
     * 강수형태 코드를 제공하지 않으므로 분석용으로 단순화합니다.
     */
    private String convertPrecipitationType(
            Double rainAmount,
            Double snowDepth,
            Double newSnowDepth
    ) {
        boolean hasRain =
                rainAmount != null && rainAmount > 0;

        boolean hasSnow =
                (snowDepth != null && snowDepth > 0)
                        || (newSnowDepth != null
                        && newSnowDepth > 0);

        if (hasRain && hasSnow) {
            return "비/눈";
        }

        if (hasSnow) {
            return "눈";
        }

        if (hasRain) {
            return "비";
        }

        return "없음";
    }

    /**
     * 빈 문자열 또는 누락된 값을 null로 변환
     */
    private Double getNullableDouble(
            JsonNode item,
            String fieldName
    ) {
        JsonNode valueNode = item.get(fieldName);

        if (valueNode == null
                || valueNode.isNull()
                || valueNode.asText().isBlank()) {
            return null;
        }

        try {
            return valueNode.asDouble();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 조회 기간 검증
     */
    private void validateDates(
            LocalDate startDate,
            LocalDate endDate
    ) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException(
                    "시작일과 종료일은 필수입니다."
            );
        }

        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException(
                    "시작일은 종료일보다 늦을 수 없습니다."
            );
        }

        LocalDate yesterday =
                LocalDate.now(KOREA_ZONE).minusDays(1);

        if (endDate.isAfter(yesterday)) {
            throw new IllegalArgumentException(
                    "과거 ASOS 시간자료는 전일까지 조회할 수 있습니다. "
                            + "조회 가능한 마지막 날짜: "
                            + yesterday
            );
        }

        long days =
                ChronoUnit.DAYS.between(startDate, endDate) + 1;

        // API 과호출과 지나치게 큰 응답을 방지합니다.
        if (days > 31) {
            throw new IllegalArgumentException(
                    "한 번에 최대 31일까지 조회할 수 있습니다."
            );
        }
    }
}