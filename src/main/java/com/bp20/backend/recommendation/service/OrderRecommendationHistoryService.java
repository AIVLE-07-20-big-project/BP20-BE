package com.bp20.backend.recommendation.service;

import com.bp20.backend.recommendation.dto.OrderRecommendationHistoryResponse;
import com.bp20.backend.recommendation.dto.OrderRecommendationResponse;
import com.bp20.backend.recommendation.entity.OrderRecommendationHistory;
import com.bp20.backend.recommendation.repository.OrderRecommendationHistoryRepository;
import com.bp20.backend.weather.dto.WeatherResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderRecommendationHistoryService {

    private final OrderRecommendationHistoryRepository historyRepository;

    /**
     * 한 번의 발주 추천 결과 전체를 저장한다.
     *
     * @return 추천 요청을 구분하는 requestId
     */
    @Transactional
    public String saveAll(
            List<OrderRecommendationResponse> recommendations,
            WeatherResponse weather,
            LocalDateTime orderDateTime
    ) {
        if (recommendations == null || recommendations.isEmpty()) {
            return null;
        }

        String requestId = UUID.randomUUID().toString();

        List<OrderRecommendationHistory> histories =
                recommendations.stream()
                        .map(recommendation ->
                                toEntity(
                                        requestId,
                                        recommendation,
                                        weather,
                                        orderDateTime
                                )
                        )
                        .toList();

        historyRepository.saveAll(histories);

        return requestId;
    }

    public List<OrderRecommendationHistoryResponse> findAll() {
        return historyRepository
                .findAllByOrderByCreatedAtDesc()
                .stream()
                .map(OrderRecommendationHistoryResponse::from)
                .toList();
    }

    public OrderRecommendationHistoryResponse findById(Long id) {
        OrderRecommendationHistory history =
                historyRepository.findById(id)
                        .orElseThrow(() ->
                                new NoSuchElementException(
                                        "발주 추천 이력을 찾을 수 없습니다. id=" + id
                                )
                        );

        return OrderRecommendationHistoryResponse.from(history);
    }

    public List<OrderRecommendationHistoryResponse> findByRequestId(
            String requestId
    ) {
        return historyRepository
                .findByRequestIdOrderByIngredientNameAsc(requestId)
                .stream()
                .map(OrderRecommendationHistoryResponse::from)
                .toList();
    }

    public List<OrderRecommendationHistoryResponse> searchByIngredientName(
            String ingredientName
    ) {
        return historyRepository
                .findByIngredientNameContainingOrderByCreatedAtDesc(
                        ingredientName
                )
                .stream()
                .map(OrderRecommendationHistoryResponse::from)
                .toList();
    }

    public List<OrderRecommendationHistoryResponse> findRequiredOrders() {
        return historyRepository
                .findByOrderRequiredTrueOrderByCreatedAtDesc()
                .stream()
                .map(OrderRecommendationHistoryResponse::from)
                .toList();
    }

    private OrderRecommendationHistory toEntity(
            String requestId,
            OrderRecommendationResponse recommendation,
            WeatherResponse weather,
            LocalDateTime orderDateTime
    ) {
        return new OrderRecommendationHistory(
                requestId,
                recommendation.ingredientName(),
                recommendation.currentStock(),
                recommendation.reservedStock(),
                recommendation.availableStock(),
                recommendation.incomingStock(),
                recommendation.safetyStock(),
                recommendation.expectedUsage(),
                recommendation.recommendedOrderQuantity(),
                recommendation.orderRequired(),
                recommendation.confidenceScore(),
                recommendation.modelName(),
                recommendation.recommendationReason(),

                weather.latitude(),
                weather.longitude(),
                weather.temperature(),
                weather.windSpeed(),
                weather.sky(),
                weather.precipitationType(),
                weather.rainProbability(),
                weather.humidity(),

                orderDateTime,
                weather.forecastDateTime()
        );
    }
}