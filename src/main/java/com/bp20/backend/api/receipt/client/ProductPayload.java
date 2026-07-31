package com.bp20.backend.api.receipt.client;

import com.bp20.backend.api.order.domain.MenuItem;

public record ProductPayload(
        Long productId,
        Long storeId,
        String productName,
        String category,
        Long price,
        Integer discountRate
) {
    public static ProductPayload from(MenuItem menuItem) {
        return new ProductPayload(
                menuItem.getId(),
                menuItem.getStore().getId(),
                menuItem.getProductName(),
                menuItem.getCategory(),
                menuItem.getPrice(),
                menuItem.getDiscountRate()
        );
    }
}
