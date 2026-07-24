package com.bp20.backend.api.product.dto.response;

import com.bp20.backend.api.product.domain.OnlineSalesItemStatus;
import com.bp20.backend.api.product.domain.Product;
import com.bp20.backend.api.product.domain.ProductStatus;

import java.time.LocalDateTime;

public record ProductResponse(
        Long id,
        String name,
        String description,
        long price,
        int stockQuantity,
        String imageUrl,
        ProductStatus status,
        OnlineSalesItemStatus onlineSalesStatus,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStockQuantity(),
                product.getImageUrl(),
                product.getStatus(),
                product.getOnlineSalesStatus(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}
