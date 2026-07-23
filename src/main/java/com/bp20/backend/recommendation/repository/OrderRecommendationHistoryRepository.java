package com.bp20.backend.recommendation.repository;

import com.bp20.backend.recommendation.entity.OrderRecommendationHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRecommendationHistoryRepository
        extends JpaRepository<OrderRecommendationHistory, Long> {

    List<OrderRecommendationHistory> findAllByOrderByCreatedAtDesc();

    List<OrderRecommendationHistory>
    findByIngredientNameContainingOrderByCreatedAtDesc(
            String ingredientName
    );

    List<OrderRecommendationHistory>
    findByRequestIdOrderByIngredientNameAsc(
            String requestId
    );

    List<OrderRecommendationHistory>
    findByOrderRequiredTrueOrderByCreatedAtDesc();
}