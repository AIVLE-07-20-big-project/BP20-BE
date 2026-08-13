package com.bp20.backend.api.review.controller;

import com.bp20.backend.api.review.dto.request.TestReviewBatchCreateRequest;
import com.bp20.backend.api.review.dto.request.TestReviewCreateRequest;
import com.bp20.backend.api.review.dto.response.ReviewResponseDto;
import com.bp20.backend.api.review.service.ReviewService;
import com.bp20.backend.global.security.principal.SecurityPrincipal;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Review", description = "리뷰 조회 및 관리 API")
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

    @PostMapping("/{storeId}/reviews/test")
    public ResponseEntity<ReviewResponseDto> createTestReview(
            @PathVariable Long storeId,
            @AuthenticationPrincipal SecurityPrincipal currentUser,
            @Valid @RequestBody TestReviewCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reviewService.createTestReview(storeId, currentUser.id(), request));
    }

    @PostMapping("/{storeId}/reviews/test/batch")
    public ResponseEntity<List<ReviewResponseDto>> createTestReviews(
            @PathVariable Long storeId,
            @AuthenticationPrincipal SecurityPrincipal currentUser,
            @Valid @RequestBody TestReviewBatchCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reviewService.createTestReviews(storeId, currentUser.id(), request.reviews()));
    }
}
