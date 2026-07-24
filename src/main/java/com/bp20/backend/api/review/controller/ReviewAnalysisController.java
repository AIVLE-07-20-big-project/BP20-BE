package com.bp20.backend.api.review.controller;

import com.bp20.backend.api.review.service.ReviewAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v3/stores")
@RequiredArgsConstructor
public class ReviewAnalysisController {
    private final ReviewAnalysisService reviewAnalysisService;

    @PostMapping("/{storeId}/reviews/analysis")
    public ResponseEntity<String> reviewAnalyseRequest (@PathVariable Long storeId) {
        reviewAnalysisService.analyzeUnanalyzedReviews(storeId);
        return ResponseEntity.ok("리뷰 ABSA 분석 요청 처리 완료");
    }

//    @GetMapping

}
