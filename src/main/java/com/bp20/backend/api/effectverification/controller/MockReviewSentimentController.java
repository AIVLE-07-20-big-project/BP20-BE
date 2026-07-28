package com.bp20.backend.api.effectverification.controller;

import com.bp20.backend.api.effectverification.dto.response.ReviewAnalysisStorageResponse;
import com.bp20.backend.api.effectverification.dto.response.ReviewBatchAnalysisResponse;
import com.bp20.backend.api.effectverification.service.MockReviewSentimentService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

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

    @PostMapping("/analyze-pending")
    public ResponseEntity<ReviewBatchAnalysisResponse> analyzePending(
            @RequestParam(name = "store_id") Long storeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime to
    ) {
        return ResponseEntity.ok(
                sentimentService.analyzePending(storeId, from, to)
        );
    }
}
