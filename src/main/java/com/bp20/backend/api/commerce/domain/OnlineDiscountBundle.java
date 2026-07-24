package com.bp20.backend.api.commerce.domain;

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
        name = "online_discount_bundles",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_online_discount_bundles_discount_bundle",
                columnNames = {"online_discount_id", "product_bundle_id"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OnlineDiscountBundle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "online_discount_bundle_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "online_discount_id", nullable = false)
    private OnlineDiscount discount;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_bundle_id", nullable = false)
    private ProductBundle bundle;

    private OnlineDiscountBundle(OnlineDiscount discount, ProductBundle bundle) {
        this.discount = discount;
        this.bundle = bundle;
    }

    public static OnlineDiscountBundle create(OnlineDiscount discount, ProductBundle bundle) {
        return new OnlineDiscountBundle(discount, bundle);
    }
}
