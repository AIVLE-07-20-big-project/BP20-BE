package com.bp20.backend.product.dto;

import com.bp20.backend.product.entity.Product;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductResponse(
        Long productId,
        String productName,
        String category,
        String unit,
        BigDecimal purchasePrice,
        BigDecimal sellingPrice,
        Integer safetyStock,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getProductId(),
                product.getProductName(),
                product.getCategory(),
                product.getUnit(),
                product.getPurchasePrice(),
                product.getSellingPrice(),
                product.getSafetyStock(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}