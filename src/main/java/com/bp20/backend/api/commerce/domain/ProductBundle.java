package com.bp20.backend.api.commerce.domain;

import com.bp20.backend.api.product.domain.OnlineSalesItemStatus;
import com.bp20.backend.api.product.domain.Product;
import com.bp20.backend.api.store.domain.Store;
import com.bp20.backend.global.domain.BaseTimeEntity;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Getter
@Entity
@Table(
        name = "product_bundles",
        indexes = @Index(name = "idx_product_bundles_store_status", columnList = "store_id,status")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductBundle extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_bundle_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(name = "bundle_price", nullable = false)
    private long bundlePrice;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BundleStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "online_sales_status", nullable = false, length = 20)
    private OnlineSalesItemStatus onlineSalesStatus;

    @OneToMany(mappedBy = "bundle", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<BundleItem> items = new ArrayList<>();

    private ProductBundle(
            Store store,
            String name,
            String description,
            long bundlePrice,
            String imageUrl
    ) {
        this.store = store;
        this.name = name;
        this.description = description;
        this.bundlePrice = bundlePrice;
        this.imageUrl = imageUrl;
        this.status = BundleStatus.DRAFT;
        this.onlineSalesStatus = OnlineSalesItemStatus.NOT_REGISTERED;
    }

    public static ProductBundle create(
            Store store,
            String name,
            String description,
            long bundlePrice,
            String imageUrl
    ) {
        return new ProductBundle(store, name, description, bundlePrice, imageUrl);
    }

    public void update(String name, String description, long bundlePrice, String imageUrl) {
        this.name = name;
        this.description = description;
        this.bundlePrice = bundlePrice;
        this.imageUrl = imageUrl;
    }

    public void replaceItems(List<BundleItemSpec> itemSpecs) {
        Map<Long, BundleItem> existingItems = new HashMap<>();
        for (BundleItem item : items) {
            existingItems.put(item.getProduct().getId(), item);
        }

        Set<Long> requestedProductIds = new HashSet<>();
        for (BundleItemSpec spec : itemSpecs) {
            Long productId = spec.product().getId();
            requestedProductIds.add(productId);

            BundleItem existingItem = existingItems.get(productId);
            if (existingItem != null) {
                existingItem.changeQuantity(spec.quantity());
                continue;
            }
            items.add(BundleItem.create(this, spec.product(), spec.quantity()));
        }

        items.removeIf(item -> !requestedProductIds.contains(item.getProduct().getId()));
    }

    public void changeStatus(BundleStatus status) {
        this.status = status;
        if (status != BundleStatus.ON_SALE && onlineSalesStatus == OnlineSalesItemStatus.ON_SALE) {
            this.onlineSalesStatus = OnlineSalesItemStatus.HIDDEN;
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

    public record BundleItemSpec(Product product, int quantity) {
    }
}
