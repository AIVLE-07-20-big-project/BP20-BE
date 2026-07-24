package com.bp20.backend.api.commerce.dto.response;

import com.bp20.backend.api.product.domain.Product;

public record DiscountProductResponse(
        Long id,
        String name,
        long price
) {
    public static DiscountProductResponse from(Product product) {
        return new DiscountProductResponse(product.getId(), product.getName(), product.getPrice());
    }
}
