package com.bp20.backend.api.product.domain;

import com.bp20.backend.api.store.domain.Store;
import com.bp20.backend.global.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "products",
        indexes = {
                @Index(name = "idx_products_store_status", columnList = "store_id,status"),
                @Index(name = "idx_products_online_status", columnList = "store_id,online_sales_status"),
                @Index(name = "idx_products_name", columnList = "name")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false)
    private long price;

    @Column(name = "stock_quantity", nullable = false)
    private int stockQuantity;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProductStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "online_sales_status", nullable = false, length = 20)
    private OnlineSalesItemStatus onlineSalesStatus;

    private Product(
            Store store,
            String name,
            String description,
            long price,
            int stockQuantity,
            String imageUrl
    ) {
        this.store = store;
        this.name = name;
        this.description = description;
        this.price = price;
        this.stockQuantity = stockQuantity;
        this.imageUrl = imageUrl;
        this.status = stockQuantity == 0 ? ProductStatus.SOLD_OUT : ProductStatus.ACTIVE;
        this.onlineSalesStatus = OnlineSalesItemStatus.NOT_REGISTERED;
    }

    public static Product create(
            Store store,
            String name,
            String description,
            long price,
            int stockQuantity,
            String imageUrl
    ) {
        return new Product(
                store,
                name,
                description,
                price,
                stockQuantity,
                imageUrl
        );
    }

    public void update(
            String name,
            String description,
            long price,
            int stockQuantity,
            String imageUrl
    ) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.stockQuantity = stockQuantity;
        this.imageUrl = imageUrl;

        if (stockQuantity == 0) {
            this.status = ProductStatus.SOLD_OUT;
            if (isRegisteredOnline()) {
                this.onlineSalesStatus = OnlineSalesItemStatus.SOLD_OUT;
            }
        } else if (this.status == ProductStatus.SOLD_OUT) {
            this.status = ProductStatus.ACTIVE;
            if (this.onlineSalesStatus == OnlineSalesItemStatus.SOLD_OUT) {
                this.onlineSalesStatus = OnlineSalesItemStatus.HIDDEN;
            }
        }
    }

    public void changeStatus(ProductStatus status) {
        this.status = status;
        if (status == ProductStatus.INACTIVE && onlineSalesStatus == OnlineSalesItemStatus.ON_SALE) {
            this.onlineSalesStatus = OnlineSalesItemStatus.HIDDEN;
        }
        if (status == ProductStatus.SOLD_OUT && isRegisteredOnline()) {
            this.onlineSalesStatus = OnlineSalesItemStatus.SOLD_OUT;
        }
    }

    public void registerOnline() {
        this.onlineSalesStatus = OnlineSalesItemStatus.ON_SALE;
    }

    public void changeOnlineSalesStatus(OnlineSalesItemStatus status) {
        this.onlineSalesStatus = status;
    }

    public void unregisterOnline() {
        this.onlineSalesStatus = OnlineSalesItemStatus.NOT_REGISTERED;
    }

    public boolean isRegisteredOnline() {
        return onlineSalesStatus != OnlineSalesItemStatus.NOT_REGISTERED;
    }
}
