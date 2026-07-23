package com.bp20.backend.recommendation.service;

import com.bp20.backend.csv.CsvDataService;
import com.bp20.backend.forecast.ForecastClient;
import com.bp20.backend.forecast.ForecastRequest;
import com.bp20.backend.forecast.ProductForecastResponse;
import com.bp20.backend.inventory.InventoryDataRequest;
import com.bp20.backend.product.ProductDataRequest;
import com.bp20.backend.sales.DailySalesDto;
import com.bp20.backend.recommendation.dto.OrderRecommendationResponse;
import com.bp20.backend.weather.dto.WeatherResponse;
import com.bp20.backend.weather.service.WeatherService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderRecommendationService {

    /**
     * 예측에 사용할 과거 매출 기간
     */
    private static final int HISTORY_DAYS = 30;

    /**
     * AI가 예측할 향후 기간
     */
    private static final int FORECAST_DAYS = 2;

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final CsvDataService csvDataService;
    private final ForecastClient forecastClient;
    private final WeatherService weatherService;
    private final OrderRecommendationHistoryService historyService;

    /**
     * 상품, 매출, 재고 및 날씨 데이터를 바탕으로
     * 재료별 발주 추천 결과를 생성한다.
     *
     * @param latitude      매장 위도
     * @param longitude     매장 경도
     * @param orderDateTime 발주 시점
     * @return 재료별 발주 추천 결과
     */
    public List<OrderRecommendationResponse> generate(
            double latitude,
            double longitude,
            LocalDateTime orderDateTime
    ) {
        LocalDateTime targetOrderDateTime =
                orderDateTime == null
                        ? LocalDateTime.now()
                        : orderDateTime;

        List<WeatherResponse> weatherForecasts =
                weatherService.getTodayAndTomorrowWeather(
                        latitude,
                        longitude,
                        targetOrderDateTime
                );

        return generate(
                latitude,
                longitude,
                targetOrderDateTime,
                weatherForecasts
        );
    }

    public List<OrderRecommendationResponse> generate(
            double latitude,
            double longitude,
            LocalDateTime orderDateTime,
            List<WeatherResponse> weatherForecasts
    ) {
        LocalDateTime targetOrderDateTime =
                orderDateTime == null
                        ? LocalDateTime.now()
                        : orderDateTime;

        LocalDate referenceDate =
                targetOrderDateTime.toLocalDate();

        /*
         * 1. 업로드된 CSV 데이터 조회
         */
        List<ProductDataRequest> products =
                csvDataService.getProducts();

        List<DailySalesDto> sales =
                csvDataService.getSales();

        List<InventoryDataRequest> inventories =
                csvDataService.getInventories();

        validateUploadedData(
                products,
                sales,
                inventories
        );

        /*
         * 2. 발주시점의 날씨 조회
         */
        WeatherResponse weather = combineWeatherForecasts(
                latitude,
                longitude,
                targetOrderDateTime,
                weatherForecasts
        );

        validateWeather(weather);

        log.info("===== AI 발주 분석 시작 =====");
        log.info("발주시점: {}", targetOrderDateTime);
        log.info("위도: {}, 경도: {}", latitude, longitude);

        log.info("===== AI 발주 날씨 데이터 =====");
        log.info("예보시점: {}", weather.forecastDateTime());
        log.info("기온: {}℃", weather.temperature());
        log.info("풍속: {}m/s", weather.windSpeed());
        log.info("하늘상태: {}", weather.sky());
        log.info("강수형태: {}", weather.precipitationType());
        log.info("강수확률: {}%", weather.rainProbability());
        log.info("습도: {}%", weather.humidity());

        /*
         * 3. 최근 30일 매출 기간 설정
         *
         * 예:
         * 발주 기준일이 2026-07-21이라면
         * 2026-06-21 ~ 2026-07-20 데이터를 사용한다.
         */
        LocalDate startDate =
                referenceDate.minusDays(HISTORY_DAYS);

        LocalDate endDate =
                referenceDate.minusDays(1);

        /*
         * 4. 예측에 사용할 날짜 범위의 매출만 추출
         */
        List<DailySalesDto> filteredSales =
                sales.stream()
                        .filter(sale ->
                                !sale.saleDate().isBefore(startDate)
                                        && !sale.saleDate().isAfter(endDate)
                        )
                        .toList();

        if (filteredSales.isEmpty()) {
            throw new IllegalStateException(
                    startDate
                            + "부터 "
                            + endDate
                            + "까지의 매출 데이터가 없습니다."
            );
        }

        log.info("상품 데이터 개수: {}", products.size());
        log.info("전체 매출 데이터 개수: {}", sales.size());
        log.info("예측용 매출 데이터 개수: {}", filteredSales.size());
        log.info("재고 데이터 개수: {}", inventories.size());

        /*
         * 5. 상품 코드별 매출 데이터 그룹화
         */
        Map<String, List<DailySalesDto>> salesByProduct =
                filteredSales.stream()
                        .collect(Collectors.groupingBy(
                                DailySalesDto::productCode
                        ));

        /*
         * 6. 상품, 매출, 날씨 데이터를 AI 요청 DTO로 변환
         */
        ForecastRequest request =
                createForecastRequest(
                        products,
                        salesByProduct,
                        weather,
                        targetOrderDateTime
                );

        log.info(
                "FastAPI 예측 요청 생성 완료: forecastDays={}, productCount={}",
                request.forecastDays(),
                request.products().size()
        );

        /*
         * 7. Python FastAPI 예측 서버 호출
         */
        ProductForecastResponse forecastResponse =
                forecastClient.predict(request);

        validateForecastResponse(forecastResponse);

        /*
         * 상품 코드를 통해 상품 정보를 빠르게 조회하기 위한 Map
         */
        Map<String, ProductDataRequest> productMap =
                products.stream()
                        .collect(Collectors.toMap(
                                ProductDataRequest::productCode,
                                Function.identity(),
                                (first, second) -> first
                        ));

        /*
         * 재료별 향후 예상 사용량
         */
        Map<String, Long> expectedUsageMap =
                new HashMap<>();

        /*
         * 재료별 예측 신뢰도
         */
        Map<String, List<Double>> confidenceMap =
                new HashMap<>();

        /*
         * 8. 상품 판매량 예측 결과를 재료 사용량으로 환산
         */
        for (
                ProductForecastResponse.ProductForecast forecast
                : forecastResponse.forecasts()
        ) {
            ProductDataRequest product =
                    productMap.get(forecast.productCode());

            /*
             * 예측 결과에 존재하지만 상품 CSV에는 없는 상품은 제외
             */
            if (product == null) {
                log.warn(
                        "상품 CSV에서 상품 코드를 찾을 수 없습니다: {}",
                        forecast.productCode()
                );
                continue;
            }

            if (product.getIngredients() == null) {
                log.warn(
                        "상품의 재료 정보가 없습니다: {}",
                        product.productCode()
                );
                continue;
            }

            for (String ingredient : product.getIngredients()) {
                if (ingredient == null || ingredient.isBlank()) {
                    continue;
                }

                /*
                 * 현재 구조에서는 상품 1개 판매 시
                 * 각 재료가 1개씩 사용되는 것으로 계산한다.
                 *
                 * 추후 상품별 레시피 수량이 존재한다면
                 * predictedSalesQuantity × 재료 사용량으로 변경해야 한다.
                 */
                expectedUsageMap.merge(
                        ingredient,
                        forecast.predictedSalesQuantity(),
                        Long::sum
                );

                confidenceMap
                        .computeIfAbsent(
                                ingredient,
                                key -> new ArrayList<>()
                        )
                        .add(forecast.confidenceScore());
            }
        }

        /*
         * 9. 재고 상태를 고려하여 최종 발주 수량 계산
         */
        List<OrderRecommendationResponse> result =
                new ArrayList<>();

        for (InventoryDataRequest inventory : inventories) {
            long expectedUsage =
                    expectedUsageMap.getOrDefault(
                            inventory.ingredientName(),
                            0L
                    );

            /*
             * 추천 발주량 계산식
             *
             * 예상 사용량
             * + 안전재고
             * - 가용재고
             * - 입고 예정 재고
             */
            long rawQuantity =
                    expectedUsage
                            + inventory.safetyStock()
                            - inventory.availableStock()
                            - inventory.incomingStock();

            long recommendedQuantity =
                    Math.max(0, rawQuantity);

            /*
             * 발주 단위에 맞게 올림
             *
             * 예:
             * 추천량 13, 발주 단위 10 → 20
             */
            recommendedQuantity =
                    roundUp(
                            recommendedQuantity,
                            inventory.orderUnit()
                    );

            double confidence =
                    average(
                            confidenceMap.get(
                                    inventory.ingredientName()
                            )
                    );

            /*
             * 현재는 프로젝트의 LlmClient 메서드 구조가 확인되지 않았으므로
             * 기본 추천 사유를 생성한다.
             *
             * LLM 연결 시 이 부분에서 LlmClient를 호출하면 된다.
             */
            String reason =
                    createRecommendationReason(
                            inventory,
                            expectedUsage,
                            recommendedQuantity,
                            weather
                    );

            result.add(
                    new OrderRecommendationResponse(
                            inventory.ingredientName(),
                            inventory.currentStock(),
                            inventory.reservedStock(),
                            inventory.availableStock(),
                            inventory.incomingStock(),
                            inventory.safetyStock(),
                            expectedUsage,
                            recommendedQuantity,
                            recommendedQuantity > 0,
                            confidence,
                            forecastResponse.selectedModel(),
                            reason
                    )
            );
        }

        String requestId =
                historyService.saveAll(
                        result,
                        weather,
                        targetOrderDateTime
                );

        log.info(
                "===== AI 발주 분석 완료: 추천 결과 {}개, requestId={} =====",
                result.size(),
                requestId
        );

        return result;
    }

    /**
     * 오늘·내일 예보를 기존 예측 서버가 받을 수 있는 하나의 날씨 특성으로 합칩니다.
     * 강수확률은 재고 부족 위험을 줄이기 위해 두 날짜 중 큰 값을 사용합니다.
     */
    private WeatherResponse combineWeatherForecasts(
            double latitude,
            double longitude,
            LocalDateTime orderDateTime,
            List<WeatherResponse> forecasts
    ) {
        if (forecasts == null || forecasts.isEmpty()) {
            throw new IllegalStateException("오늘·내일 날씨 데이터가 없습니다.");
        }

        WeatherResponse first = forecasts.get(0);

        return new WeatherResponse(
                orderDateTime,
                first.forecastDateTime(),
                latitude,
                longitude,
                first.nx(),
                first.ny(),
                averageNullable(forecasts.stream().map(WeatherResponse::temperature).toList()),
                averageNullable(forecasts.stream().map(WeatherResponse::windSpeed).toList()),
                forecasts.stream()
                        .map(WeatherResponse::sky)
                        .filter(value -> value != null && !value.isBlank())
                        .reduce((ignored, value) -> value)
                        .orElse(null),
                forecasts.stream()
                        .map(WeatherResponse::precipitationType)
                        .filter(value -> value != null && !value.equals("없음"))
                        .findFirst()
                        .orElse("없음"),
                forecasts.stream()
                        .map(WeatherResponse::rainProbability)
                        .filter(java.util.Objects::nonNull)
                        .max(Integer::compareTo)
                        .orElse(null),
                averageNullableInteger(
                        forecasts.stream().map(WeatherResponse::humidity).toList()
                )
        );
    }

    private Double averageNullable(List<Double> values) {
        return values.stream()
                .filter(java.util.Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .average()
                .stream()
                .boxed()
                .findFirst()
                .orElse(null);
    }

    private Integer averageNullableInteger(List<Integer> values) {
        return values.stream()
                .filter(java.util.Objects::nonNull)
                .mapToInt(Integer::intValue)
                .average()
                .stream()
                .mapToObj(value -> (int) Math.round(value))
                .findFirst()
                .orElse(null);
    }

    /**
     * FastAPI에 전달할 예측 요청을 생성한다.
     *
     * createForecastRequest는 별도 클래스에 추가하는 것이 아니라
     * OrderRecommendationService 내부의 private 메서드로 둔다.
     */
    private ForecastRequest createForecastRequest(
            List<ProductDataRequest> products,
            Map<String, List<DailySalesDto>> salesByProduct,
            WeatherResponse weather,
            LocalDateTime orderDateTime
    ) {

        ForecastRequest.WeatherFeature weatherFeature =
                new ForecastRequest.WeatherFeature(
                        weather.forecastDateTime() == null
                                ? null
                                : weather.forecastDateTime()
                                .format(DATE_TIME_FORMATTER),
                        weather.temperature(),
                        weather.windSpeed(),
                        weather.sky(),
                        weather.precipitationType(),
                        weather.rainProbability(),
                        weather.humidity()
                );

        List<ForecastRequest.ProductSalesHistory> productHistories =
                products.stream()
                        .map(product -> {

                            List<ForecastRequest.DailySalesValue> salesHistory =
                                    salesByProduct
                                            .getOrDefault(
                                                    product.productCode(),
                                                    List.of()
                                            )
                                            .stream()
                                            .map(sale ->
                                                    new ForecastRequest.DailySalesValue(
                                                            sale.saleDate()
                                                                    .format(DATE_FORMATTER),
                                                            sale.salesQuantity(),
                                                            sale.unitPrice()
                                                    )
                                            )
                                            .toList();

                            return new ForecastRequest.ProductSalesHistory(
                                    product.productCode(),
                                    product.productName(),
                                    salesHistory
                            );
                        })
                        .toList();

        return new ForecastRequest(
                FORECAST_DAYS,
                orderDateTime.format(DATE_TIME_FORMATTER),
                weatherFeature,
                productHistories
        );
    }

    /**
     * 업로드된 CSV 데이터가 존재하는지 확인한다.
     */
    private void validateUploadedData(
            List<ProductDataRequest> products,
            List<DailySalesDto> sales,
            List<InventoryDataRequest> inventories
    ) {
        if (products == null || products.isEmpty()) {
            throw new IllegalStateException(
                    "상품 CSV를 먼저 업로드해야 합니다."
            );
        }

        if (sales == null || sales.isEmpty()) {
            throw new IllegalStateException(
                    "매출 CSV를 먼저 업로드해야 합니다."
            );
        }

        if (inventories == null || inventories.isEmpty()) {
            throw new IllegalStateException(
                    "재고 CSV를 먼저 업로드해야 합니다."
            );
        }
    }

    /**
     * 날씨 API 응답을 검증한다.
     */
    private void validateWeather(WeatherResponse weather) {
        if (weather == null) {
            throw new IllegalStateException(
                    "날씨 데이터를 조회하지 못했습니다."
            );
        }
    }

    /**
     * FastAPI 예측 응답을 검증한다.
     */
    private void validateForecastResponse(
            ProductForecastResponse forecastResponse
    ) {
        if (forecastResponse == null) {
            throw new IllegalStateException(
                    "AI 예측 서버에서 응답을 받지 못했습니다."
            );
        }

        if (forecastResponse.forecasts() == null) {
            throw new IllegalStateException(
                    "AI 예측 서버 응답에 forecasts가 없습니다."
            );
        }
    }

    /**
     * 날씨를 포함한 기본 추천 사유를 생성한다.
     */
    private String createRecommendationReason(
            InventoryDataRequest inventory,
            long expectedUsage,
            long recommendedQuantity,
            WeatherResponse weather
    ) {
        return String.format(
                "%s의 향후 예상 사용량은 %d개입니다. "
                        + "현재 재고, 입고 예정 수량, 안전재고와 "
                        + "기온 %s℃, 강수확률 %s%%의 날씨를 고려하여 "
                        + "%d개 발주를 추천합니다.",
                inventory.ingredientName(),
                expectedUsage,
                valueOrUnknown(weather.temperature()),
                valueOrUnknown(weather.rainProbability()),
                recommendedQuantity
        );
    }

    /**
     * null 값을 문자열로 안전하게 변환한다.
     */
    private String valueOrUnknown(Object value) {
        return value == null
                ? "정보 없음"
                : value.toString();
    }

    /**
     * 발주 수량을 발주 단위에 맞게 올림한다.
     */
    private long roundUp(
            long quantity,
            long orderUnit
    ) {
        if (quantity <= 0) {
            return 0;
        }

        if (orderUnit <= 1) {
            return quantity;
        }

        return ((quantity + orderUnit - 1) / orderUnit)
                * orderUnit;
    }

    /**
     * 신뢰도 목록의 평균을 계산한다.
     */
    private double average(List<Double> values) {
        if (values == null || values.isEmpty()) {
            return 0.0;
        }

        return values.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
    }
}
