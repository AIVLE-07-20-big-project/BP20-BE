package com.bp20.backend.api.budget.controller;

import com.bp20.backend.api.budget.dto.request.BudgetCreateRequest;
import com.bp20.backend.api.budget.dto.response.BudgetResponse;
import com.bp20.backend.api.budget.service.BudgetService;
import com.bp20.backend.global.response.ApiResponse;
import com.bp20.backend.global.response.SuccessCode;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Budget", description = "카테고리별 월 예산 목표치 API")
@RestController
@RequestMapping("/api/store-owner/budgets")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class BudgetController {

    private final BudgetService budgetService;

    @PostMapping
    public ResponseEntity<ApiResponse<BudgetResponse>> create(
            @Valid @RequestBody BudgetCreateRequest request
    ) {
        BudgetResponse result = budgetService.createOrUpdateBudget(request);
        return ApiResponse.success(SuccessCode.SUCCESS_BUDGET_CREATE, result);
    }
}