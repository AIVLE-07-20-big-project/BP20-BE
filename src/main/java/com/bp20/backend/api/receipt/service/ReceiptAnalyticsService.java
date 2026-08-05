package com.bp20.backend.api.receipt.service;

import com.bp20.backend.api.budget.domain.Budget;
import com.bp20.backend.api.budget.repository.BudgetRepository;
import com.bp20.backend.api.commerce.repository.DiscountRepository;
import com.bp20.backend.api.order.domain.Order;
import com.bp20.backend.api.order.repository.OrderRepository;
import com.bp20.backend.api.product.domain.Product;
import com.bp20.backend.api.product.repository.ProductRepository;
import com.bp20.backend.api.receipt.client.OcrServiceClient;
import com.bp20.backend.api.receipt.domain.Receipt;
import com.bp20.backend.api.receipt.domain.ReceiptItem;
import com.bp20.backend.api.receipt.dto.response.BudgetOverageResponse;
import com.bp20.backend.api.receipt.dto.response.ExpenseAnomalyResponse;
import com.bp20.backend.api.receipt.repository.ReceiptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    private final ProductRepository productRepository;
    private final DiscountRepository discountRepository;
    private final OrderRepository orderRepository;
    private final OcrServiceClient ocrServiceClient;

    public List<ExpenseAnomalyResponse> getExpenseAnomalies(Long storeId, double zThreshold) {
        List<Receipt> receipts =
                receiptRepository.findByStore_IdOrderByTransactionDateDesc(storeId);
        return ocrServiceClient.getExpenseAnomalies(receipts, zThreshold);
    }

    public List<BudgetOverageResponse> getBudgetOverage(Long storeId) {
        List<Receipt> receipts =
                receiptRepository.findByStore_IdOrderByTransactionDateDesc(storeId);
        List<Budget> budgets = budgetRepository.findByStore_Id(storeId);
        return ocrServiceClient.getBudgetOverage(receipts, budgets);
    }

    public String getReport(Long storeId, String storeName, String reportType, Integer year, Integer month) {
        List<Receipt> receipts =
                receiptRepository.findByStore_IdOrderByTransactionDateDesc(storeId);
        List<Budget> budgets = budgetRepository.findByStore_Id(storeId);
        List<ReceiptItem> items = receipts.stream()
                .flatMap(receipt -> receipt.getItems().stream())
                .toList();
        List<Product> products = productRepository.findByStoreIdOrderByIdDesc(storeId);
        List<Order> orders = orderRepository.findByStoreIdOrderByOrderedDateDesc(storeId);
        Map<Long, Integer> discountRatesByProductId = discountRepository
                .findActiveRateDiscountsByStore(storeId, LocalDateTime.now())
                .stream()
                .collect(Collectors.toMap(
                        discount -> discount.getProduct().getId(),
                        discount -> (int) discount.getDiscountValue(),
                        Math::max
                ));

        return ocrServiceClient.getReport(
                receipts, budgets, items, products, orders, discountRatesByProductId,
                storeName, reportType, year, month
        );
    }
}
