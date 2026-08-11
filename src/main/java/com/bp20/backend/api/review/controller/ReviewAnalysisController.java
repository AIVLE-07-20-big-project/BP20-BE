package com.bp20.backend.api.review.controller;

import com.bp20.backend.api.review.dto.response.AspectRadarResponseDto;
import com.bp20.backend.api.review.dto.response.AspectStatResponseDto;
import com.bp20.backend.api.review.dto.response.MonthlyReportStatusResponseDto;
import com.bp20.backend.api.review.service.ReviewAnalysisService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.time.YearMonth;

@Tag(name = "Review", description = "리뷰 조회 및 관리 API")
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

    @PostMapping("/{storeId}/reviews/monthly-report")
    public ResponseEntity<Void> generateMonthlyReport(
            @PathVariable Long storeId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth targetMonth
    ) {
        reviewAnalysisService.generateMonthlyRecommendation(storeId, targetMonth);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{storeId}/reviews/monthly-report/status")
    public ResponseEntity<MonthlyReportStatusResponseDto> getMonthlyReportStatus(
            @PathVariable Long storeId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth targetMonth
    ) {
        return ResponseEntity.ok(reviewAnalysisService.getMonthlyReportStatus(storeId, targetMonth));
    }

    @GetMapping("/{storeId}/aspect-scores")
    public ResponseEntity<List<AspectRadarResponseDto>> getAspectScores(
            @PathVariable("storeId") Long storeId
    ) {
        List<AspectRadarResponseDto> response = reviewAnalysisService.getAspectRadarChart(storeId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{storeId}/aspect-stat")
    public ResponseEntity<List<AspectStatResponseDto>> getAspectStat(
            @PathVariable("storeId") Long storeId
    ) {
        List<AspectStatResponseDto> response = reviewAnalysisService.getAspectStats(storeId);
        return ResponseEntity.ok(response);
    }

}
