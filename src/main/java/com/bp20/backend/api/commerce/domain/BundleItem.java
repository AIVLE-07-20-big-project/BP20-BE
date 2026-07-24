package com.bp20.backend.api.commerce.domain;

import com.bp20.backend.api.product.domain.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "bundle_items",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_bundle_items_bundle_product",
                columnNames = {"product_bundle_id", "product_id"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BundleItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bundle_item_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_bundle_id", nullable = false)
    private ProductBundle bundle;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private int quantity;

    private BundleItem(ProductBundle bundle, Product product, int quantity) {
        this.bundle = bundle;
        this.product = product;
        this.quantity = quantity;
    }

    public static BundleItem create(ProductBundle bundle, Product product, int quantity) {
        return new BundleItem(bundle, product, quantity);
    }

    public void changeQuantity(int quantity) {
        this.quantity = quantity;
    }
}
