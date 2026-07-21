package com.bp20.backend.effectverification.controller;

import com.bp20.backend.effectverification.dto.response.RecommendationStrategyWeightResponse;
import com.bp20.backend.effectverification.service.MockRecommendationFeedbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Profile("mock")
@RequestMapping("/api/mock/recommendation-strategy-weights")
@RequiredArgsConstructor
public class MockRecommendationFeedbackController {

    private final MockRecommendationFeedbackService feedbackService;

    @GetMapping
    public ResponseEntity<List<RecommendationStrategyWeightResponse>> getByStore(
            @RequestParam(name = "store_id") Long storeId
    ) {
        return ResponseEntity.ok(feedbackService.getByStore(storeId));
    }
}
