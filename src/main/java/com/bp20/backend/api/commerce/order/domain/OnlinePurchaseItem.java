package com.bp20.backend.api.commerce.order.domain;

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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "online_purchase_items")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OnlinePurchaseItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "online_purchase_item_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "online_purchase_id", nullable = false)
    private OnlinePurchase onlinePurchase;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "product_name", nullable = false, length = 120)
    private String productName;

    @Column(name = "unit_price", nullable = false)
    private long unitPrice;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "line_amount", nullable = false)
    private long lineAmount;

    private OnlinePurchaseItem(OnlinePurchase onlinePurchase, Product product, int quantity) {
        this.onlinePurchase = onlinePurchase;
        this.product = product;
        this.productName = product.getName();
        this.unitPrice = product.getPrice();
        this.quantity = quantity;
        this.lineAmount = Math.multiplyExact(product.getPrice(), quantity);
    }

    public static OnlinePurchaseItem create(OnlinePurchase purchase, Product product, int quantity) {
        return new OnlinePurchaseItem(purchase, product, quantity);
    }
}
