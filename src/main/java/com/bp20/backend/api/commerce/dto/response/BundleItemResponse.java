package com.bp20.backend.api.commerce.dto.response;

import com.bp20.backend.api.commerce.domain.BundleItem;

public record BundleItemResponse(
        Long productId,
        String productName,
        long unitPrice,
        int quantity
) {
    public static BundleItemResponse from(BundleItem item) {
        return new BundleItemResponse(
                item.getProduct().getId(),
                item.getProduct().getName(),
                item.getProduct().getPrice(),
                item.getQuantity()
        );
    }
}
