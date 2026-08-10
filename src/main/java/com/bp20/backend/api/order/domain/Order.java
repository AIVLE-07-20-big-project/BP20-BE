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

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 매장의 판매 주문(매출) 1건. AI 가계부 리포트의 "매출" 합계·추이 계산에 쓰인다.
 * recommendation 패키지의 OrderRecommendation*(재고 발주 추천)과는 전혀 다른 개념이다.
 * cafe_sales_orders.csv 실측 컬럼에는 CustomerID가 없어 이 엔티티에도 포함하지 않았다.
 */
@Getter
@Entity
@Table(
        name = "orders",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_orders_store_source_order",
                columnNames = {"store_id", "source_order_id"}
        ),
        indexes = @Index(name = "idx_orders_store_date", columnList = "store_id,ordered_date")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    /**
     * cafe_sales_orders.csv의 OrderID 원본 값. 주문상세(OrderItem) CSV 임포트 시
     * 이 값으로 어느 주문에 속하는지 다시 찾는다.
     */
    @Column(name = "source_order_id", nullable = false)
    private Long sourceOrderId;

    @Column(name = "order_type", nullable = false, length = 20)
    private String orderType;

    @Column(name = "total_amount", nullable = false)
    private long totalAmount;

    @Column(name = "discount_amount", nullable = false)
    private long discountAmount;

    @Column(name = "payment_method", length = 20)
    private String paymentMethod;

    @Column(name = "ordered_date", nullable = false)
    private LocalDate orderedDate;

    @Column(name = "ordered_time")
    private LocalTime orderedTime;

    private Order(
            Store store,
            Long sourceOrderId,
            String orderType,
            long totalAmount,
            long discountAmount,
            String paymentMethod,
            LocalDate orderedDate,
            LocalTime orderedTime
    ) {
        this.store = store;
        this.sourceOrderId = sourceOrderId;
        this.orderType = orderType;
        this.totalAmount = totalAmount;
        this.discountAmount = discountAmount;
        this.paymentMethod = paymentMethod;
        this.orderedDate = orderedDate;
        this.orderedTime = orderedTime;
    }

    public static Order create(
            Store store,
            Long sourceOrderId,
            String orderType,
            long totalAmount,
            long discountAmount,
            String paymentMethod,
            LocalDate orderedDate,
            LocalTime orderedTime
    ) {
        return new Order(store, sourceOrderId, orderType, totalAmount, discountAmount, paymentMethod, orderedDate, orderedTime);
    }
}
