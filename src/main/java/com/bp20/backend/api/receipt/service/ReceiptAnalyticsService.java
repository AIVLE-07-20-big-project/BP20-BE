package com.bp20.backend.api.receipt.service;

import com.bp20.backend.api.budget.domain.Budget;
import com.bp20.backend.api.budget.repository.BudgetRepository;
import com.bp20.backend.api.receipt.client.OcrServiceClient;
import com.bp20.backend.api.receipt.domain.Receipt;
import com.bp20.backend.api.receipt.domain.ReceiptItem;
import com.bp20.backend.api.receipt.dto.response.BudgetOverageResponse;
import com.bp20.backend.api.receipt.dto.response.ExpenseAnomalyResponse;
import com.bp20.backend.api.receipt.repository.ReceiptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 2번(AI 가계부) 기능: 실제 통계 계산은 Python 서비스(OcrServiceClient)에 위임하고,
 * 이 서비스는 DB에서 데이터를 모아 넘겨주는 역할만 한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReceiptAnalyticsService {

    private final ReceiptRepository receiptRepository;
    private final BudgetRepository budgetRepository;
    private final OcrServiceClient ocrServiceClient;

    public List<ExpenseAnomalyResponse> getExpenseAnomalies(Long storeId, double zThreshold) {
        List<Receipt> receipts = receiptRepository.findByStoreIdOrderByTransactionDateDesc(storeId);
        return ocrServiceClient.getExpenseAnomalies(receipts, zThreshold);
    }

    public List<BudgetOverageResponse> getBudgetOverage(Long storeId) {
        List<Receipt> receipts = receiptRepository.findByStoreIdOrderByTransactionDateDesc(storeId);
        List<Budget> budgets = budgetRepository.findByStoreId(storeId);
        return ocrServiceClient.getBudgetOverage(receipts, budgets);
    }

    public String getReport(Long storeId, String storeName, String reportType, Integer year, Integer month) {
        List<Receipt> receipts = receiptRepository.findByStoreIdOrderByTransactionDateDesc(storeId);
        List<Budget> budgets = budgetRepository.findByStoreId(storeId);
        List<ReceiptItem> items = receipts.stream()
                .flatMap(receipt -> receipt.getItems().stream())
                .toList();

        return ocrServiceClient.getReport(receipts, budgets, items, storeName, reportType, year, month);
    }
}