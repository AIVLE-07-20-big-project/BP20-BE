package com.bp20.backend.effectverification.controller;

import com.bp20.backend.effectverification.dto.response.ReviewAnalysisStorageResponse;
import com.bp20.backend.effectverification.service.MockReviewSentimentService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("mock")
@RequestMapping("/api/mock/reviews")
@RequiredArgsConstructor
public class MockReviewSentimentController {

    private final MockReviewSentimentService sentimentService;

    @PostMapping("/{reviewId}/analyze")
    public ResponseEntity<ReviewAnalysisStorageResponse> analyze(
            @PathVariable Long reviewId
    ) {
        return ResponseEntity.ok(
                sentimentService.analyzeAndStore(reviewId)
        );
    }
}

