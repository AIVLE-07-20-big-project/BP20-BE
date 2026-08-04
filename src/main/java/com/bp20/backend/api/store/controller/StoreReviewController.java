package com.bp20.backend.api.store.controller;

import com.bp20.backend.api.store.dto.response.StoreRecommendationResponseDto;
import com.bp20.backend.api.store.dto.response.StoreReviewKeywordResponseDto;
import com.bp20.backend.api.store.service.StoreReviewKeywordService;
import com.bp20.backend.api.store.service.StoreReviewRecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v3/stores")
@RequiredArgsConstructor
public class StoreReviewController {

    private final StoreReviewKeywordService storeReviewKeywordService;
    private final StoreReviewRecommendationService storeReviewRecommendationService;

    @GetMapping("/{storeId}/reviews/keywords")
    public ResponseEntity<List<StoreReviewKeywordResponseDto>> getStoreReviewKeywords(
            @PathVariable Long storeId
    ) {
        List<StoreReviewKeywordResponseDto> response = storeReviewKeywordService.getStoreKeywords(storeId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "최신 AI 추천 개선사항 조회", description = "해당 매장의 가장 최근 AI 개선 추천 리포트를 조회합니다.")
    @GetMapping("/{storeId}/recommendations/latest")
    public ResponseEntity<StoreRecommendationResponseDto> getLatestRecommendation(
            @PathVariable("storeId") Long storeId
    ) {
        StoreRecommendationResponseDto response = storeReviewRecommendationService.getLatestRecommendation(storeId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/recommendations/{recommendationId}/action-items/complete")
    public ResponseEntity<Void> completeActionItem(
            @PathVariable("recommendationId") Long recommendationId,
            @RequestParam("keyword") String keyword
    ) {
        storeReviewRecommendationService.completeActionItem(recommendationId, keyword);
        return ResponseEntity.ok().build();
    }

}
