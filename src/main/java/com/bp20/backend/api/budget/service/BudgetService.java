package com.bp20.backend.api.budget.service;

import com.bp20.backend.api.budget.domain.Budget;
import com.bp20.backend.api.budget.dto.request.BudgetCreateRequest;
import com.bp20.backend.api.budget.dto.response.BudgetResponse;
import com.bp20.backend.api.budget.repository.BudgetRepository;
import com.bp20.backend.api.store.domain.Store;
import com.bp20.backend.api.store.repository.StoreRepository;
import com.bp20.backend.global.exception.ApiException;
import com.bp20.backend.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final StoreRepository storeRepository;

    /**
     * 같은 (storeId, yearMonth, category) 조합이 이미 있으면 금액만 갱신(upsert)하고,
     * 없으면 새로 만든다. CSV 일괄 등록 시 여러 번 실행해도 중복 행이 쌓이지 않게 하기 위함.
     */
    @Transactional
    public BudgetResponse createOrUpdateBudget(BudgetCreateRequest request) {
        Store store = storeRepository.findById(request.storeId())
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND_STORE));

        Budget budget = budgetRepository
                .findByStore_IdAndYearMonthAndCategory(
                        request.storeId(),
                        request.yearMonth(),
                        request.category()
                )
                .map(existing -> {
                    existing.updateAmount(request.budgetAmount());
                    return existing;
                })
                .orElseGet(() -> Budget.create(
                        store,
                        request.yearMonth(),
                        request.category(),
                        request.budgetAmount()
                ));

        Budget saved = budgetRepository.save(budget);
        return BudgetResponse.from(saved);
    }
}
