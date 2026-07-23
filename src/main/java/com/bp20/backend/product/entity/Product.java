package com.bp20.backend.product.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "products",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_product_name",
                        columnNames = "product_name"
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long productId;

    @Column(name = "product_name", nullable = false, length = 100)
    private String productName;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(nullable = false, length = 30)
    private String unit;

    @Column(
            name = "purchase_price",
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal purchasePrice;

    @Column(
            name = "selling_price",
            precision = 12,
            scale = 2
    )
    private BigDecimal sellingPrice;

    @Column(name = "safety_stock", nullable = false)
    private Integer safetyStock;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Product(
            String productName,
            String category,
            String unit,
            BigDecimal purchasePrice,
            BigDecimal sellingPrice,
            Integer safetyStock
    ) {
        this.productName = productName;
        this.category = category;
        this.unit = unit;
        this.purchasePrice = purchasePrice;
        this.sellingPrice = sellingPrice;
        this.safetyStock = safetyStock;
    }

    public void update(
            String productName,
            String category,
            String unit,
            BigDecimal purchasePrice,
            BigDecimal sellingPrice,
            Integer safetyStock
    ) {
        this.productName = productName;
        this.category = category;
        this.unit = unit;
        this.purchasePrice = purchasePrice;
        this.sellingPrice = sellingPrice;
        this.safetyStock = safetyStock;
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();

        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}