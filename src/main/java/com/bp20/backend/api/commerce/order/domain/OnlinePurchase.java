package com.bp20.backend.api.commerce.order.domain;

import com.bp20.backend.api.customer.domain.Customer;
import com.bp20.backend.api.product.domain.Product;
import com.bp20.backend.api.store.domain.Store;
import com.bp20.backend.global.domain.BaseTimeEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
@Entity
@Table(
        name = "online_purchases",
        indexes = {
                @Index(name = "idx_online_purchases_store_date", columnList = "store_id,purchased_at"),
                @Index(name = "idx_online_purchases_customer", columnList = "customer_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OnlinePurchase extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "online_purchase_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "purchased_at", nullable = false)
    private LocalDateTime purchasedAt;

    @Column(name = "total_amount", nullable = false)
    private long totalAmount;

    @OneToMany(mappedBy = "onlinePurchase", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OnlinePurchaseItem> items = new ArrayList<>();

    private OnlinePurchase(Store store, Customer customer, LocalDateTime purchasedAt, long totalAmount) {
        this.store = store;
        this.customer = customer;
        this.purchasedAt = purchasedAt;
        this.totalAmount = totalAmount;
    }

    public static OnlinePurchase create(
            Store store,
            Customer customer,
            LocalDateTime purchasedAt,
            long totalAmount
    ) {
        return new OnlinePurchase(store, customer, purchasedAt, totalAmount);
    }

    public void addItem(Product product, int quantity) {
        items.add(OnlinePurchaseItem.create(this, product, quantity));
    }

    public List<OnlinePurchaseItem> getItems() {
        return Collections.unmodifiableList(items);
    }
}
