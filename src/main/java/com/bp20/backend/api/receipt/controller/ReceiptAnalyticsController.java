package com.bp20.backend.api.receipt.controller;

import com.bp20.backend.api.receipt.dto.response.BudgetOverageResponse;
import com.bp20.backend.api.receipt.dto.response.ExpenseAnomalyResponse;
import com.bp20.backend.api.receipt.service.ReceiptAnalyticsService;
import com.bp20.backend.global.response.ApiResponse;
import com.bp20.backend.global.response.SuccessCode;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "ReceiptAnalytics", description = "AI 가계부(이상지출/예산초과/리포트) API")
@RestController
@RequestMapping("/api/store-owner/analytics")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class ReceiptAnalyticsController {

    private final ReceiptAnalyticsService analyticsService;

    @GetMapping("/expense-anomalies")
    public ResponseEntity<ApiResponse<List<ExpenseAnomalyResponse>>> expenseAnomalies(
            @RequestParam Long storeId,
            @RequestParam(defaultValue = "1.3") double zThreshold
    ) {
        List<ExpenseAnomalyResponse> result = analyticsService.getExpenseAnomalies(storeId, zThreshold);
        return ApiResponse.success(SuccessCode.SUCCESS_ANALYTICS_EXPENSE_ANOMALIES, result);
    }

    @GetMapping("/budget-overage")
    public ResponseEntity<ApiResponse<List<BudgetOverageResponse>>> budgetOverage(
            @RequestParam Long storeId
    ) {
        List<BudgetOverageResponse> result = analyticsService.getBudgetOverage(storeId);
        return ApiResponse.success(SuccessCode.SUCCESS_ANALYTICS_BUDGET_OVERAGE, result);
    }

    /**
     * 경영 장부 HTML 리포트. 다른 엔드포인트와 달리 ApiResponse로 감싸지 않고
     * HTML을 그대로 반환한다 (브라우저에서 바로 열어보는 용도).
     *
     * 예) /api/analytics/report?storeId=1&reportType=yearly&year=2025
     */
    @GetMapping(value = "/report", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> report(
            @RequestParam Long storeId,
            @RequestParam(defaultValue = "매장") String storeName,
            @RequestParam(defaultValue = "full") String reportType,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month
    ) {
        String html = analyticsService.getReport(storeId, storeName, reportType, year, month);
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
    }
}