package com.bp20.backend.api.commerce.dto.response;

import com.bp20.backend.api.commerce.domain.ProductBundle;

public record OnlineDiscountBundleResponse(
        Long id,
        String name,
        long bundlePrice
) {
    public static OnlineDiscountBundleResponse from(ProductBundle bundle) {
        return new OnlineDiscountBundleResponse(
                bundle.getId(),
                bundle.getName(),
                bundle.getBundlePrice()
        );
    }
}
