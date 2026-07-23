package com.bp20.backend.recommendation.controller;

import com.bp20.backend.recommendation.dto.OrderRecommendationHistoryResponse;
import com.bp20.backend.recommendation.service.OrderRecommendationHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/order-recommendations/history")
@RequiredArgsConstructor
public class OrderRecommendationHistoryController {

    private final OrderRecommendationHistoryService historyService;

    /**
     * 전체 추천 이력 조회
     */
    @GetMapping
    public ResponseEntity<List<OrderRecommendationHistoryResponse>> findAll(
            @RequestParam(required = false) String ingredientName,
            @RequestParam(required = false) Boolean orderRequired
    ) {
        if (ingredientName != null && !ingredientName.isBlank()) {
            return ResponseEntity.ok(
                    historyService.searchByIngredientName(ingredientName)
            );
        }

        if (Boolean.TRUE.equals(orderRequired)) {
            return ResponseEntity.ok(
                    historyService.findRequiredOrders()
            );
        }

        return ResponseEntity.ok(
                historyService.findAll()
        );
    }

    /**
     * 추천 이력 단건 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<OrderRecommendationHistoryResponse> findById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                historyService.findById(id)
        );
    }

    /**
     * 한 번의 추천 요청에서 생성된 결과 전체 조회
     */
    @GetMapping("/request/{requestId}")
    public ResponseEntity<List<OrderRecommendationHistoryResponse>>
    findByRequestId(
            @PathVariable String requestId
    ) {
        return ResponseEntity.ok(
                historyService.findByRequestId(requestId)
        );
    }
}