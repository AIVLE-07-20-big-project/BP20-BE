package com.bp20.backend.recommendation.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "order_recommendation_history",
        indexes = {
                @Index(
                        name = "idx_recommendation_ingredient",
                        columnList = "ingredient_name"
                ),
                @Index(
                        name = "idx_recommendation_order_datetime",
                        columnList = "order_datetime"
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderRecommendationHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 같은 추천 요청에서 생성된 결과들을 묶기 위한 값
     */
    @Column(name = "request_id", nullable = false, length = 50)
    private String requestId;

    @Column(name = "ingredient_name", nullable = false, length = 100)
    private String ingredientName;

    @Column(name = "current_stock", nullable = false)
    private long currentStock;

    @Column(name = "reserved_stock", nullable = false)
    private long reservedStock;

    @Column(name = "available_stock", nullable = false)
    private long availableStock;

    @Column(name = "incoming_stock", nullable = false)
    private long incomingStock;

    @Column(name = "safety_stock", nullable = false)
    private long safetyStock;

    @Column(name = "expected_usage", nullable = false)
    private long expectedUsage;

    @Column(name = "recommended_order_quantity", nullable = false)
    private long recommendedOrderQuantity;

    @Column(name = "order_required", nullable = false)
    private boolean orderRequired;

    @Column(name = "confidence_score", nullable = false)
    private double confidenceScore;

    @Column(name = "model_name", length = 100)
    private String modelName;

    @Column(name = "recommendation_reason", columnDefinition = "TEXT")
    private String recommendationReason;

    @Column(name = "latitude", nullable = false)
    private double latitude;

    @Column(name = "longitude", nullable = false)
    private double longitude;

    @Column(name = "temperature")
    private Double temperature;

    @Column(name = "wind_speed")
    private Double windSpeed;

    @Column(name = "sky", length = 30)
    private String sky;

    @Column(name = "precipitation_type", length = 30)
    private String precipitationType;

    @Column(name = "rain_probability")
    private Integer rainProbability;

    @Column(name = "humidity")
    private Integer humidity;

    @Column(name = "order_datetime", nullable = false)
    private LocalDateTime orderDateTime;

    @Column(name = "forecast_datetime")
    private LocalDateTime forecastDateTime;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public OrderRecommendationHistory(
            String requestId,
            String ingredientName,
            long currentStock,
            long reservedStock,
            long availableStock,
            long incomingStock,
            long safetyStock,
            long expectedUsage,
            long recommendedOrderQuantity,
            boolean orderRequired,
            double confidenceScore,
            String modelName,
            String recommendationReason,
            double latitude,
            double longitude,
            Double temperature,
            Double windSpeed,
            String sky,
            String precipitationType,
            Integer rainProbability,
            Integer humidity,
            LocalDateTime orderDateTime,
            LocalDateTime forecastDateTime
    ) {
        this.requestId = requestId;
        this.ingredientName = ingredientName;
        this.currentStock = currentStock;
        this.reservedStock = reservedStock;
        this.availableStock = availableStock;
        this.incomingStock = incomingStock;
        this.safetyStock = safetyStock;
        this.expectedUsage = expectedUsage;
        this.recommendedOrderQuantity = recommendedOrderQuantity;
        this.orderRequired = orderRequired;
        this.confidenceScore = confidenceScore;
        this.modelName = modelName;
        this.recommendationReason = recommendationReason;
        this.latitude = latitude;
        this.longitude = longitude;
        this.temperature = temperature;
        this.windSpeed = windSpeed;
        this.sky = sky;
        this.precipitationType = precipitationType;
        this.rainProbability = rainProbability;
        this.humidity = humidity;
        this.orderDateTime = orderDateTime;
        this.forecastDateTime = forecastDateTime;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}