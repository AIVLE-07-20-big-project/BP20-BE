package com.bp20.backend.weather.service;

import com.bp20.backend.weather.dto.WeatherResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
//@RequiredArgsConstructor
public class WeatherService {

    private static final ZoneId KOREA_ZONE =
            ZoneId.of("Asia/Seoul");

    private static final DateTimeFormatter API_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd");

    private static final DateTimeFormatter API_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HHmm");

    private static final DateTimeFormatter FORECAST_DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMddHHmm");

    /**
     * 기상청 단기예보 발표시각입니다.
     */
    private static final int[] BASE_HOURS = {
            2, 5, 8, 11,
            14, 17, 20, 23
    };

    private final RestClient restClient;

    @Value("${weather.api.service-key}")
    private String serviceKey;

    @Value("${kma.forecast-url}")
    private String forecastUrl;

    public WeatherService(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    /**
     * 발주시점과 가장 가까운 예보시간의 날씨를 반환합니다.
     */
    public WeatherResponse getOrderWeather(
            double latitude,
            double longitude,
            LocalDateTime orderDateTime
    ) {
        validateCoordinates(
                latitude,
                longitude
        );

        if (orderDateTime == null) {
            throw new IllegalArgumentException(
                    "발주시점은 필수입니다."
            );
        }

        Grid grid = convertToGrid(
                latitude,
                longitude
        );

        /*
         * 발주시점을 기준으로 사용할 수 있는
         * 가장 최근 기상청 발표자료를 계산합니다.
         */
        BaseDateTime baseDateTime =
                getBaseDateTimeForOrder(orderDateTime);

        URI requestUri =
                createForecastRequestUri(
                        grid,
                        baseDateTime
                );

        Map<String, Object> apiResponse =
                requestForecast(requestUri);

        validateApiResponse(apiResponse);

        return extractOrderWeather(
                apiResponse,
                latitude,
                longitude,
                grid,
                orderDateTime
        );
    }

    /**
     * 현재 위치의 오늘과 내일 예보를 한 번의 기상청 API 호출로 조회합니다.
     * 두 예보는 현재 시각과 가장 가까운 시각을 기준으로 선택합니다.
     */
    public List<WeatherResponse> getTodayAndTomorrowWeather(
            double latitude,
            double longitude,
            LocalDateTime now
    ) {
        validateCoordinates(latitude, longitude);

        LocalDateTime reference = now == null
                ? LocalDateTime.now(KOREA_ZONE)
                : now;

        Grid grid = convertToGrid(latitude, longitude);
        BaseDateTime baseDateTime = getBaseDateTimeForOrder(reference);
        URI requestUri = createForecastRequestUri(grid, baseDateTime);
        Map<String, Object> apiResponse = requestForecast(requestUri);
        validateApiResponse(apiResponse);

        return List.of(
                extractOrderWeather(
                        apiResponse,
                        latitude,
                        longitude,
                        grid,
                        reference
                ),
                extractOrderWeather(
                        apiResponse,
                        latitude,
                        longitude,
                        grid,
                        reference.plusDays(1)
                )
        );
    }

    public Grid getGrid(
            double latitude,
            double longitude
    ) {
        validateCoordinates(
                latitude,
                longitude
        );

        return convertToGrid(
                latitude,
                longitude
        );
    }

    /**
     * 기상청 API 요청 주소를 생성합니다.
     */
    private URI createForecastRequestUri(
            Grid grid,
            BaseDateTime baseDateTime
    ) {
        return UriComponentsBuilder
                .fromUriString(forecastUrl)
                .queryParam(
                        "serviceKey",
                        serviceKey
                )
                .queryParam(
                        "pageNo",
                        1
                )
                .queryParam(
                        "numOfRows",
                        1000
                )
                .queryParam(
                        "dataType",
                        "JSON"
                )
                .queryParam(
                        "base_date",
                        baseDateTime.baseDate()
                )
                .queryParam(
                        "base_time",
                        baseDateTime.baseTime()
                )
                .queryParam(
                        "nx",
                        grid.nx()
                )
                .queryParam(
                        "ny",
                        grid.ny()
                )
                .build()
                .encode()
                .toUri();
    }

    /**
     * 기상청 단기예보 API를 호출합니다.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> requestForecast(
            URI requestUri
    ) {
        try {
            log.info("발주시점 날씨 조회를 위해 기상청 API를 호출합니다.");

            return restClient
                    .get()
                    .uri(requestUri)
                    .retrieve()
                    .body(Map.class);

        } catch (RestClientException e) {
            throw new IllegalStateException(
                    "기상청 API 호출에 실패했습니다.",
                    e
            );
        }
    }

    /**
     * 기상청 API 응답의 성공 여부를 확인합니다.
     */
    private void validateApiResponse(
            Map<String, Object> apiResponse
    ) {
        if (apiResponse == null) {
            throw new IllegalStateException(
                    "기상청 API 응답이 없습니다."
            );
        }

        Map<String, Object> response =
                getMap(apiResponse, "response");

        Map<String, Object> header =
                getMap(response, "header");

        String resultCode =
                getString(
                        header,
                        "resultCode"
                );

        String resultMessage =
                getString(
                        header,
                        "resultMsg"
                );

        if (!"00".equals(resultCode)) {
            throw new IllegalStateException(
                    "기상청 API 오류: "
                            + resultCode
                            + " - "
                            + resultMessage
            );
        }
    }

    /**
     * 전체 기상청 예보 중 발주시점에 해당하는
     * 시간대 하나만 선택해 WeatherResponse로 변환합니다.
     */
    private WeatherResponse extractOrderWeather(
            Map<String, Object> apiResponse,
            double latitude,
            double longitude,
            Grid grid,
            LocalDateTime orderDateTime
    ) {
        List<?> rawItems =
                getForecastItems(apiResponse);

        if (rawItems.isEmpty()) {
            throw new IllegalStateException(
                    "조회된 기상청 예보 데이터가 없습니다."
            );
        }

        /*
         * 발주시점 이후 예보 중 가장 가까운 시간대를 찾습니다.
         */
        LocalDateTime targetForecastDateTime =
                findClosestForecastDateTime(
                        rawItems,
                        orderDateTime
                );

        /*
         * 같은 예보시간의 TMP, WSD, SKY, PTY, POP, REH를
         * category -> fcstValue 형태로 저장합니다.
         */
        Map<String, String> weatherValues =
                collectWeatherValues(
                        rawItems,
                        targetForecastDateTime
                );

        return new WeatherResponse(
                orderDateTime,
                targetForecastDateTime,
                latitude,
                longitude,
                grid.nx(),
                grid.ny(),
                parseDouble(
                        weatherValues.get("TMP")
                ),
                parseDouble(
                        weatherValues.get("WSD")
                ),
                convertSkyStatus(
                        weatherValues.get("SKY")
                ),
                convertPrecipitationType(
                        weatherValues.get("PTY")
                ),
                parseInteger(
                        weatherValues.get("POP")
                ),
                parseInteger(
                        weatherValues.get("REH")
                )
        );
    }

    /**
     * 기상청 응답에서 item 배열을 추출합니다.
     */
    private List<?> getForecastItems(
            Map<String, Object> apiResponse
    ) {
        Map<String, Object> response =
                getMap(apiResponse, "response");

        Map<String, Object> body =
                getMap(response, "body");

        Object itemsObject =
                body.get("items");

        if (!(itemsObject instanceof Map<?, ?> items)) {
            return List.of();
        }

        Object itemObject =
                items.get("item");

        if (!(itemObject instanceof List<?> itemList)) {
            return List.of();
        }

        return itemList;
    }

    /**
     * 발주시점 이후의 예보 중 가장 가까운 예보시간을 선택합니다.
     *
     * 발주시점 이후 예보가 없다면 전체 예보 중
     * 발주시점과 절대 시간 차이가 가장 작은 예보를 선택합니다.
     */
    private LocalDateTime findClosestForecastDateTime(
            List<?> rawItems,
            LocalDateTime orderDateTime
    ) {
        LocalDateTime closestFuture =
                null;

        LocalDateTime closestOverall =
                null;

        long smallestFutureDifference =
                Long.MAX_VALUE;

        long smallestOverallDifference =
                Long.MAX_VALUE;

        for (Object rawItem : rawItems) {
            if (!(rawItem instanceof Map<?, ?> item)) {
                continue;
            }

            String category =
                    getString(
                            item,
                            "category"
                    );

            /*
             * 같은 시간대가 여러 카테고리만큼 반복되므로
             * TMP 항목만 사용해 시간 후보를 확인합니다.
             */
            if (!"TMP".equals(category)) {
                continue;
            }

            LocalDateTime forecastDateTime =
                    parseForecastDateTime(item);

            if (forecastDateTime == null) {
                continue;
            }

            long absoluteDifference =
                    Math.abs(
                            Duration.between(
                                    orderDateTime,
                                    forecastDateTime
                            ).toMinutes()
                    );

            if (absoluteDifference <
                    smallestOverallDifference) {

                smallestOverallDifference =
                        absoluteDifference;

                closestOverall =
                        forecastDateTime;
            }

            /*
             * 발주시점과 같거나 미래인 예보만 후보로 지정합니다.
             */
            if (!forecastDateTime.isBefore(orderDateTime)) {
                long futureDifference =
                        Duration.between(
                                orderDateTime,
                                forecastDateTime
                        ).toMinutes();

                if (futureDifference <
                        smallestFutureDifference) {

                    smallestFutureDifference =
                            futureDifference;

                    closestFuture =
                            forecastDateTime;
                }
            }
        }

        if (closestFuture != null) {
            return closestFuture;
        }

        if (closestOverall != null) {
            return closestOverall;
        }

        throw new IllegalStateException(
                "발주시점에 사용할 예보시간을 찾을 수 없습니다."
        );
    }

    /**
     * 선택한 예보시간의 필요한 날씨값만 수집합니다.
     */
    private Map<String, String> collectWeatherValues(
            List<?> rawItems,
            LocalDateTime targetForecastDateTime
    ) {
        Map<String, String> values =
                new HashMap<>();

        for (Object rawItem : rawItems) {
            if (!(rawItem instanceof Map<?, ?> item)) {
                continue;
            }

            LocalDateTime forecastDateTime =
                    parseForecastDateTime(item);

            if (!targetForecastDateTime.equals(
                    forecastDateTime
            )) {
                continue;
            }

            String category =
                    getString(
                            item,
                            "category"
                    );

            if (!isRequiredCategory(category)) {
                continue;
            }

            values.put(
                    category,
                    getString(
                            item,
                            "fcstValue"
                    )
            );
        }

        return values;
    }

    private boolean isRequiredCategory(
            String category
    ) {
        return switch (category) {
            case "TMP",
                 "WSD",
                 "SKY",
                 "PTY",
                 "POP",
                 "REH" -> true;

            default -> false;
        };
    }

    /**
     * fcstDate와 fcstTime을 LocalDateTime으로 변환합니다.
     */
    private LocalDateTime parseForecastDateTime(
            Map<?, ?> item
    ) {
        String forecastDate =
                getString(
                        item,
                        "fcstDate"
                );

        String forecastTime =
                getString(
                        item,
                        "fcstTime"
                );

        if (forecastDate == null ||
                forecastTime == null) {
            return null;
        }

        try {
            return LocalDateTime.parse(
                    forecastDate + forecastTime,
                    FORECAST_DATE_TIME_FORMATTER
            );
        } catch (Exception e) {
            log.warn(
                    "예보시각 변환 실패: {} {}",
                    forecastDate,
                    forecastTime
            );

            return null;
        }
    }

    /**
     * 발주시점 기준으로 사용할 수 있는
     * 가장 최근 기상청 발표시각을 계산합니다.
     */
    private BaseDateTime getBaseDateTimeForOrder(
            LocalDateTime orderDateTime
    ) {
        /*
         * 기상청 데이터 등록 지연을 고려해
         * 발주시점에서 10분을 뺍니다.
         */
        LocalDateTime reference =
                orderDateTime.minusMinutes(10);

        int selectedHour =
                -1;

        for (int i =
             BASE_HOURS.length - 1;
             i >= 0;
             i--) {

            if (reference.getHour() >=
                    BASE_HOURS[i]) {

                selectedHour =
                        BASE_HOURS[i];

                break;
            }
        }

        LocalDate baseDate;

        if (selectedHour == -1) {
            baseDate =
                    reference.toLocalDate()
                            .minusDays(1);

            selectedHour = 23;
        } else {
            baseDate =
                    reference.toLocalDate();
        }

        return new BaseDateTime(
                baseDate.format(
                        API_DATE_FORMATTER
                ),
                LocalTime.of(
                                selectedHour,
                                0
                        )
                        .format(
                                API_TIME_FORMATTER
                        )
        );
    }

    private String convertSkyStatus(
            String value
    ) {
        if (value == null) {
            return null;
        }

        return switch (value) {
            case "1" -> "맑음";
            case "3" -> "구름많음";
            case "4" -> "흐림";
            default -> "알 수 없음";
        };
    }

    private String convertPrecipitationType(
            String value
    ) {
        if (value == null) {
            return null;
        }

        return switch (value) {
            case "0" -> "없음";
            case "1" -> "비";
            case "2" -> "비/눈";
            case "3" -> "눈";
            case "4" -> "소나기";
            default -> "알 수 없음";
        };
    }

    private Double parseDouble(
            String value
    ) {
        if (value == null ||
                value.isBlank()) {
            return null;
        }

        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer parseInteger(
            String value
    ) {
        if (value == null ||
                value.isBlank()) {
            return null;
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 위도와 경도를 기상청 격자로 변환합니다.
     */
    private Grid convertToGrid(
            double latitude,
            double longitude
    ) {
        final double earthRadius =
                6371.00877;

        final double gridSpacing =
                5.0;

        final double standardLatitude1 =
                30.0;

        final double standardLatitude2 =
                60.0;

        final double referenceLongitude =
                126.0;

        final double referenceLatitude =
                38.0;

        final double referenceX =
                43.0;

        final double referenceY =
                136.0;

        final double degreeToRadian =
                Math.PI / 180.0;

        double re =
                earthRadius / gridSpacing;

        double slat1 =
                standardLatitude1 *
                        degreeToRadian;

        double slat2 =
                standardLatitude2 *
                        degreeToRadian;

        double olon =
                referenceLongitude *
                        degreeToRadian;

        double olat =
                referenceLatitude *
                        degreeToRadian;

        double sn =
                Math.tan(
                        Math.PI * 0.25 +
                                slat2 * 0.5
                )
                        /
                        Math.tan(
                                Math.PI * 0.25 +
                                        slat1 * 0.5
                        );

        sn =
                Math.log(
                        Math.cos(slat1) /
                                Math.cos(slat2)
                )
                        /
                        Math.log(sn);

        double sf =
                Math.tan(
                        Math.PI * 0.25 +
                                slat1 * 0.5
                );

        sf =
                Math.pow(sf, sn) *
                        Math.cos(slat1) /
                        sn;

        double ro =
                Math.tan(
                        Math.PI * 0.25 +
                                olat * 0.5
                );

        ro =
                re * sf /
                        Math.pow(ro, sn);

        double ra =
                Math.tan(
                        Math.PI * 0.25 +
                                latitude *
                                        degreeToRadian *
                                        0.5
                );

        ra =
                re * sf /
                        Math.pow(ra, sn);

        double theta =
                longitude *
                        degreeToRadian -
                        olon;

        if (theta > Math.PI) {
            theta -=
                    2.0 * Math.PI;
        }

        if (theta < -Math.PI) {
            theta +=
                    2.0 * Math.PI;
        }

        theta *= sn;

        int nx =
                (int) Math.floor(
                        ra *
                                Math.sin(theta) +
                                referenceX +
                                0.5
                );

        int ny =
                (int) Math.floor(
                        ro -
                                ra *
                                        Math.cos(theta) +
                                referenceY +
                                0.5
                );

        return new Grid(
                nx,
                ny
        );
    }

    private void validateCoordinates(
            double latitude,
            double longitude
    ) {
        if (!Double.isFinite(latitude) ||
                latitude < 30.0 ||
                latitude > 45.0) {

            throw new IllegalArgumentException(
                    "위도는 30 이상 45 이하의 숫자여야 합니다."
            );
        }

        if (!Double.isFinite(longitude) ||
                longitude < 120.0 ||
                longitude > 135.0) {

            throw new IllegalArgumentException(
                    "경도는 120 이상 135 이하의 숫자여야 합니다."
            );
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getMap(
            Map<String, Object> source,
            String key
    ) {
        Object value =
                source.get(key);

        if (!(value instanceof Map<?, ?>)) {
            throw new IllegalStateException(
                    "기상청 응답에 '"
                            + key
                            + "' 항목이 없습니다."
            );
        }

        return (Map<String, Object>) value;
    }

    private String getString(
            Map<?, ?> source,
            String key
    ) {
        Object value =
                source.get(key);

        return value == null
                ? null
                : String.valueOf(value);
    }

    public record Grid(
            int nx,
            int ny
    ) {
    }

    private record BaseDateTime(
            String baseDate,
            String baseTime
    ) {
    }
}
