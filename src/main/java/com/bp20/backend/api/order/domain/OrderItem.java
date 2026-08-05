package com.bp20.backend.api.order.domain;

import com.bp20.backend.api.product.domain.Product;
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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 주문(Order) 1건에 속한 품목 1줄. 현재 AI 가계부 리포트 계산 자체에는 쓰이지 않지만
 * (매출 합계는 Order.totalAmount만 사용), 향후 메뉴별 판매량 분석 등에 대비해 같이 저장한다.
 *
 * PR #35 리뷰 코멘트 반영: product FK로 상품을 직접 참조한다(이전에는 productName 문자열만
 * 보관). 주문상세 CSV는 반드시 메뉴(Product) CSV를 먼저 올린 뒤 업로드해야 하며,
 * OrderCsvImportService.loadOrderItems()가 sourceProductId로 Product를 찾아 연결한다.
 */
@Getter
@Entity
@Table(
        name = "order_items",
        indexes = @Index(name = "idx_order_items_order", columnList = "order_id")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_item_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "unit_price", nullable = false)
    private long unitPrice;

    @Column(name = "total_price", nullable = false)
    private long totalPrice;

    private OrderItem(
            Order order,
            Product product,
            int quantity,
            long unitPrice,
            long totalPrice
    ) {
        this.order = order;
        this.product = product;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.totalPrice = totalPrice;
    }

    public static OrderItem create(
            Order order,
            Product product,
            int quantity,
            long unitPrice,
            long totalPrice
    ) {
        return new OrderItem(order, product, quantity, unitPrice, totalPrice);
    }
}
