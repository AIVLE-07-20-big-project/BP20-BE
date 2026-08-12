package com.bp20.backend.api.commerce.order.dto.response;

import com.bp20.backend.api.commerce.order.domain.OnlinePurchase;
import com.bp20.backend.api.commerce.order.domain.OnlinePurchaseItem;
import com.bp20.backend.global.util.PersonalDataMasker;

import java.time.LocalDateTime;
import java.util.List;

public record OnlinePurchaseResponse(
        Long id,
        Long customerId,
        String customerName,
        String customerEmail,
        LocalDateTime purchasedAt,
        long totalAmount,
        List<Item> items
) {
    public static OnlinePurchaseResponse from(OnlinePurchase purchase) {
        return new OnlinePurchaseResponse(
                purchase.getId(),
                purchase.getCustomer().getId(),
                PersonalDataMasker.name(purchase.getCustomer().getName()),
                PersonalDataMasker.email(purchase.getCustomer().getEmail()),
                purchase.getPurchasedAt(),
                purchase.getTotalAmount(),
                purchase.getItems().stream().map(Item::from).toList()
        );
    }

    public record Item(
            Long productId,
            String productName,
            long unitPrice,
            int quantity,
            long lineAmount
    ) {
        private static Item from(OnlinePurchaseItem item) {
            return new Item(
                    item.getProduct().getId(),
                    item.getProductName(),
                    item.getUnitPrice(),
                    item.getQuantity(),
                    item.getLineAmount()
            );
        }
    }
}
