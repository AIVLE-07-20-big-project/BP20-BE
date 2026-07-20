package com.bp20.backend.api.budget.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record BudgetCreateRequest(

        @NotNull(message = "storeId는 필수입니다.")
        Long storeId,

        @NotBlank(message = "yearMonth는 필수입니다.")
        @Pattern(regexp = "\\d{4}-\\d{2}", message = "yearMonth는 'YYYY-MM' 형식이어야 합니다.")
        String yearMonth,

        @NotBlank(message = "category는 필수입니다.")
        String category,

        @NotNull(message = "budgetAmount는 필수입니다.")
        Integer budgetAmount
) {
}