package com.bp20.backend.api.reviewAnalysis.controller;

import com.bp20.backend.api.reviewAnalysis.dto.request.ReviewAnalysisRequestDto;
import com.bp20.backend.api.reviewAnalysis.service.ReviewAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v3/review-analysis")
@RequiredArgsConstructor
public class ReviewAnalysisController {
    private final ReviewAnalysisService reviewAnalysisService;

    @PostMapping
    public ResponseEntity<String> reviewAnalyseRequest () {
        reviewAnalysisService.analyzeUnanalyzedReviews();
        return ResponseEntity.ok("리뷰 ABSA 분석 요청 처리 완료");
    }

//    @GetMapping

}
