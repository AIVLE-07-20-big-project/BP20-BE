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
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 상품(재고/온라인판매용 + CSV로 임포트되는 매장 메뉴).
 *
 * PR #35 리뷰 코멘트 반영: 기존에는 CSV로 올리는 "메뉴"를 별도 MenuItem 엔티티로 관리했으나,
 * Product와 역할이 겹친다는 지적에 따라 Product로 통합했다. sourceProductId/category는
 * CSV(cafe_products.csv)로 임포트된 행에만 채워지고, ProductController로 직접 등록한
 * 상품은 null로 남는다 - "내 상품" 목록(ProductService.getMine)이 CSV 임포트 행까지
 * 뒤섞여 보이지 않도록 이 값으로 구분한다. 할인율은 더 이상 이 엔티티에 저장하지 않고,
 * 필요하면 Discount 테이블에서 해당 상품의 활성 할인을 조회해서 쓴다.
 */
@Getter
@Entity
@Table(
        name = "products",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_products_store_source_product",
                columnNames = {"store_id", "source_product_id"}
        ),
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

    /**
     * cafe_products.csv의 ProductID 원본 값. CSV로 임포트된 상품에만 채워진다(재업로드 시
     * 같은 매장 안에서 같은 메뉴를 다시 찾기 위한 자연키). ProductController로 직접 등록한
     * 상품은 null이다.
     */
    @Column(name = "source_product_id")
    private Long sourceProductId;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 50)
    private String category;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false)
    private long price;

    @Column(name = "stock_quantity")
    private Integer stockQuantity;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProductStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "online_sales_status", nullable = false, length = 20)
    private OnlineSalesStatus onlineSalesStatus;

    private Product(
            Store store,
            String name,
            String description,
            long price,
            Integer stockQuantity,
            String imageUrl,
            ProductStatus status
    ) {
        this.store = store;
        this.name = name;
        this.description = description;
        this.price = price;
        this.stockQuantity = stockQuantity;
        this.imageUrl = imageUrl;
        this.status = status;
        this.onlineSalesStatus = OnlineSalesStatus.NOT_REGISTERED;
    }

    public static Product create(
            Store store,
            String name,
            String description,
            long price,
            Integer stockQuantity,
            String imageUrl,
            ProductStatus status
    ) {
        return new Product(
                store,
                name,
                description,
                price,
                stockQuantity,
                imageUrl,
                status
        );
    }

    private Product(
            Store store,
            Long sourceProductId,
            String name,
            String category,
            long price
    ) {
        this.store = store;
        this.sourceProductId = sourceProductId;
        this.name = name;
        this.category = category;
        this.price = price;
        this.stockQuantity = 0;
        this.imageUrl = null;
        // CSV로 임포트되는 메뉴는 재고 개념이 없으므로(카페 메뉴), 재고 0이어도 SOLD_OUT으로
        // 취급하지 않는다 - ACTIVE/SOLD_OUT 판정은 재고를 다루는 create()/update() 쪽 로직.
        this.status = ProductStatus.ACTIVE;
        this.onlineSalesStatus = OnlineSalesStatus.NOT_REGISTERED;
    }

    /**
     * cafe_products.csv 한 행을 새 상품으로 등록한다. OrderCsvImportService에서
     * (store, sourceProductId)로 기존 행을 먼저 찾아보고, 없을 때만 호출한다.
     */
    public static Product createFromCsv(
            Store store,
            Long sourceProductId,
            String name,
            String category,
            long price
    ) {
        return new Product(store, sourceProductId, name, category, price);
    }

    /**
     * CSV 재업로드 시 이미 존재하는 (store, sourceProductId) 상품의 이름/카테고리/가격만 갱신한다.
     * id를 그대로 유지해야 이 상품을 참조하는 OrderItem/Discount FK가 끊어지지 않는다.
     */
    public void updateFromCsv(String name, String category, long price) {
        this.name = name;
        this.category = category;
        this.price = price;
    }

    public void update(
            String name,
            String description,
            long price,
            Integer stockQuantity,
            String imageUrl,
            ProductStatus status
    ) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.stockQuantity = stockQuantity;
        this.imageUrl = imageUrl;
        this.status = status;

        if (status != ProductStatus.ACTIVE && onlineSalesStatus == OnlineSalesStatus.ON_SALE) {
            unregisterOnline();
        }
    }

    public void changeStatus(ProductStatus status) {
        this.status = status;
        if (status == ProductStatus.INACTIVE && onlineSalesStatus == OnlineSalesStatus.ON_SALE) {
            unregisterOnline();
        }
        if (status == ProductStatus.SOLD_OUT && onlineSalesStatus == OnlineSalesStatus.ON_SALE) {
            unregisterOnline();
        }
    }

    public void registerOnline() {
        this.onlineSalesStatus = OnlineSalesStatus.ON_SALE;
    }

    public void unregisterOnline() {
        this.onlineSalesStatus = OnlineSalesStatus.NOT_REGISTERED;
    }

    public boolean isRegisteredOnline() {
        return onlineSalesStatus != OnlineSalesStatus.NOT_REGISTERED;
    }

    public boolean hasStockQuantity() {
        return stockQuantity != null;
    }

    public void decreaseStock(int quantity) {
        if (stockQuantity == null) {
            return;
        }
        this.stockQuantity -= quantity;
        if (this.stockQuantity == 0) {
            this.status = ProductStatus.SOLD_OUT;
            unregisterOnline();
        }
    }
}
