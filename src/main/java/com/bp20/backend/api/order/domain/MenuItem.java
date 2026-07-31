package com.bp20.backend.api.order.domain;

import com.bp20.backend.api.store.domain.Store;
import com.bp20.backend.global.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * 매장 메뉴(판매 상품) 원장 - AI 가계부 리포트의 "메뉴별 원가율" 계산에 쓰인다.
 * 재고/온라인판매용 Product(api.product 패키지)와는 스키마와 용도가 다른 별개 개념이다.
 */
@Getter
@Entity
@Table(
        name = "menu_items",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_menu_items_store_source_product",
                columnNames = {"store_id", "source_product_id"}
        ),
        indexes = @Index(name = "idx_menu_items_store", columnList = "store_id")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MenuItem extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "menu_item_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    /**
     * cafe_products.csv의 ProductID 원본 값. CSV를 다시 업로드해도 같은 매장 안에서
     * 같은 메뉴를 다시 찾을 수 있도록 자연키로 보관한다 (DB PK는 별도로 자동 생성).
     */
    @Column(name = "source_product_id", nullable = false)
    private Long sourceProductId;

    @Column(name = "product_name", nullable = false, length = 100)
    private String productName;

    @Column(length = 50)
    private String category;

    @Column(nullable = false)
    private long price;

    @Column(name = "discount_rate", nullable = false)
    private int discountRate;

    private MenuItem(
            Store store,
            Long sourceProductId,
            String productName,
            String category,
            long price,
            int discountRate
    ) {
        this.store = store;
        this.sourceProductId = sourceProductId;
        this.productName = productName;
        this.category = category;
        this.price = price;
        this.discountRate = discountRate;
    }

    public static MenuItem create(
            Store store,
            Long sourceProductId,
            String productName,
            String category,
            long price,
            int discountRate
    ) {
        return new MenuItem(store, sourceProductId, productName, category, price, discountRate);
    }
}
