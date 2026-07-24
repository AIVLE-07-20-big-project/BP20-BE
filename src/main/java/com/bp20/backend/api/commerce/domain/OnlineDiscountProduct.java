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
        name = "online_discount_products",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_online_discount_products_discount_product",
                columnNames = {"online_discount_id", "product_id"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OnlineDiscountProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "online_discount_product_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "online_discount_id", nullable = false)
    private OnlineDiscount discount;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    private OnlineDiscountProduct(OnlineDiscount discount, Product product) {
        this.discount = discount;
        this.product = product;
    }

    public static OnlineDiscountProduct create(OnlineDiscount discount, Product product) {
        return new OnlineDiscountProduct(discount, product);
    }
}
