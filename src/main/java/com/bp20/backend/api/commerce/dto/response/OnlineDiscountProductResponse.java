package com.bp20.backend.api.commerce.dto.response;

import com.bp20.backend.api.product.domain.Product;

public record OnlineDiscountProductResponse(
        Long id,
        String name,
        long price
) {
    public static OnlineDiscountProductResponse from(Product product) {
        return new OnlineDiscountProductResponse(product.getId(), product.getName(), product.getPrice());
    }
}
