package com.bp20.backend.api.commerce.dto.response;

import com.bp20.backend.api.commerce.domain.BundleStatus;
import com.bp20.backend.api.commerce.domain.ProductBundle;
import com.bp20.backend.api.product.domain.OnlineSalesItemStatus;

import java.time.LocalDateTime;
import java.util.List;

public record ProductBundleResponse(
        Long id,
        String name,
        String description,
        long regularPrice,
        long bundlePrice,
        long discountAmount,
        String imageUrl,
        BundleStatus status,
        OnlineSalesItemStatus onlineSalesStatus,
        List<BundleItemResponse> items,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ProductBundleResponse from(ProductBundle bundle) {
        long regularPrice = bundle.getItems().stream()
                .mapToLong(item -> item.getProduct().getPrice() * item.getQuantity())
                .sum();
        return new ProductBundleResponse(
                bundle.getId(),
                bundle.getName(),
                bundle.getDescription(),
                regularPrice,
                bundle.getBundlePrice(),
                regularPrice - bundle.getBundlePrice(),
                bundle.getImageUrl(),
                bundle.getStatus(),
                bundle.getOnlineSalesStatus(),
                bundle.getItems().stream().map(BundleItemResponse::from).toList(),
                bundle.getCreatedAt(),
                bundle.getUpdatedAt()
        );
    }
}
