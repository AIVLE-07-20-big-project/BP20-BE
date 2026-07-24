package com.bp20.backend.api.review.controller;

import com.bp20.backend.api.review.domain.Review;
import com.bp20.backend.api.review.dto.response.ReviewResponseDto;
import com.bp20.backend.api.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v3/stores")
@RequiredArgsConstructor
public class ReviewController {
    private final ReviewService reviewService;

    @GetMapping("/{storeId}/reviews")
    public ResponseEntity<List<ReviewResponseDto>> getReviewsByStoreId(@PathVariable Long storeId) {
        List<ReviewResponseDto> reviews = reviewService.getReviewsByStoreId(storeId);
        return ResponseEntity.ok(reviews);
    }
}
