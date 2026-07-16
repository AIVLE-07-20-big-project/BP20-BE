package com.bp20.backend.recommendation;

import com.bp20.backend.csv.CsvDataService;
import com.bp20.backend.forecast.*;
import com.bp20.backend.inventory.IngredientInventoryData;
import com.bp20.backend.llm.LlmClient;
import com.bp20.backend.llm.LlmReasonRequest;
import com.bp20.backend.product.ProductData;
import com.bp20.backend.sales.DailySaleData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderRecommendationService {

    private static final int HISTORY_DAYS = 30;
    private static final int FORECAST_DAYS = 7;

    private final CsvDataService csvDataService;
    private final ForecastClient forecastClient;
    private final LlmClient llmClient;

    public List<OrderRecommendationResponse> generate(
            LocalDate referenceDate
    ) {
        List<ProductData> products =
                csvDataService.getProducts();

        List<DailySaleData> sales =
                csvDataService.getSales();

        List<IngredientInventoryData> inventories =
                csvDataService.getInventories();

        validateUploadedData(
                products,
                sales,
                inventories
        );

        LocalDate startDate =
                referenceDate.minusDays(HISTORY_DAYS);

        LocalDate endDate =
                referenceDate.minusDays(1);

        /*
         * 예측에 사용할 날짜 범위의 판매 데이터만 추출한다.
         */
        List<DailySaleData> filteredSales =
                sales.stream()
                        .filter(sale ->
                                !sale.saleDate().isBefore(startDate)
                                        && !sale.saleDate().isAfter(endDate)
                        )
                        .toList();

        /*
         * 상품별 판매 이력 그룹화
         */
        Map<String, List<DailySaleData>> salesByProduct =
                filteredSales.stream()
                        .collect(Collectors.groupingBy(
                                DailySaleData::productCode
                        ));

        ForecastRequest request =
                createForecastRequest(
                        products,
                        salesByProduct
                );

        /*
         * Python FastAPI에서 LightGBM/XGBoost 실행
         */
        ProductForecastResponse forecastResponse =
                forecastClient.predict(request);

        Map<String, ProductData> productMap =
                products.stream()
                        .collect(Collectors.toMap(
                                ProductData::productCode,
                                Function.identity(),
                                (first, second) -> first
                        ));

        /*
         * 재료별 향후 예상 사용량
         */
        Map<String, Long> expectedUsageMap =
                new HashMap<>();

        Map<String, List<Double>> confidenceMap =
                new HashMap<>();

        for (
                ProductForecastResponse.ProductForecast forecast
                : forecastResponse.forecasts()
        ) {
            ProductData product =
                    productMap.get(forecast.productCode());

            if (product == null) {
                continue;
            }

            for (String ingredient : product.getIngredients()) {
                /*
                 * 상품 1개 판매 시 재료 1개 소모
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

        List<OrderRecommendationResponse> result =
                new ArrayList<>();

        for (
                IngredientInventoryData inventory
                : inventories
        ) {
            long expectedUsage =
                    expectedUsageMap.getOrDefault(
                            inventory.ingredientName(),
                            0L
                    );

            long rawQuantity =
                    expectedUsage
                            + inventory.safetyStock()
                            - inventory.availableStock()
                            - inventory.incomingStock();

            long recommendedQuantity =
                    Math.max(0, rawQuantity);

            recommendedQuantity = roundUp(
                    recommendedQuantity,
                    inventory.orderUnit()
            );

            double confidence =
                    average(
                            confidenceMap.get(
                                    inventory.ingredientName()
                            )
                    );

            LlmReasonRequest llmRequest =
                    new LlmReasonRequest(
                            inventory.ingredientName(),
                            inventory.currentStock(),
                            inventory.incomingStock(),
                            inventory.safetyStock(),
                            expectedUsage,
                            recommendedQuantity,
                            confidence,
                            forecastResponse.selectedModel()
                    );

            String reason = String.format(
                    "%s의 향후 예상 사용량은 %d개입니다. "
                            + "현재 재고와 안전재고를 고려하여 %d개 발주를 추천합니다.",
                    inventory.ingredientName(),
                    expectedUsage,
                    recommendedQuantity
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

        return result;
    }

    private ForecastRequest createForecastRequest(
            List<ProductData> products,
            Map<String, List<DailySaleData>> salesByProduct
    ) {
        List<ForecastRequest.ProductSalesHistory> histories =
                products.stream()
                        .map(product -> {
                            List<ForecastRequest.DailySalesValue> history =
                                    salesByProduct
                                            .getOrDefault(
                                                    product.productCode(),
                                                    List.of()
                                            )
                                            .stream()
                                            .sorted(
                                                    Comparator.comparing(
                                                            DailySaleData::saleDate
                                                    )
                                            )
                                            .map(sale ->
                                                    new ForecastRequest.DailySalesValue(
                                                            sale.saleDate(),
                                                            sale.salesQuantity(),
                                                            sale.unitPrice()
                                                    )
                                            )
                                            .toList();

                            return new ForecastRequest.ProductSalesHistory(
                                    product.productCode(),
                                    product.productName(),
                                    history
                            );
                        })
                        .toList();

        return new ForecastRequest(
                FORECAST_DAYS,
                histories
        );
    }

    private void validateUploadedData(
            List<ProductData> products,
            List<DailySaleData> sales,
            List<IngredientInventoryData> inventories
    ) {
        if (products.isEmpty()) {
            throw new IllegalStateException(
                    "상품 CSV를 먼저 업로드해야 합니다."
            );
        }

        if (sales.isEmpty()) {
            throw new IllegalStateException(
                    "매출 CSV를 먼저 업로드해야 합니다."
            );
        }

        if (inventories.isEmpty()) {
            throw new IllegalStateException(
                    "재고 CSV를 먼저 업로드해야 합니다."
            );
        }
    }

    private long roundUp(long quantity, long orderUnit) {
        if (quantity <= 0) {
            return 0;
        }

        if (orderUnit <= 1) {
            return quantity;
        }

        return ((quantity + orderUnit - 1) / orderUnit)
                * orderUnit;
    }

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