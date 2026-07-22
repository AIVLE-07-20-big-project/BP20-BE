package com.bp20.backend.api.receipt.domain;

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

/**
 * 영수증 내 품목 1건. MatchedProductId는 아직 Product 엔티티가 없어 순수 컬럼(Long, nullable)으로 둔다.
 * (나중에 원가 분석에서 재고 품목과 매칭할 때 채워질 필드)
 */
@Getter
@Entity
@Table(name = "receipt_items")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReceiptItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "receipt_item_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receipt_id", nullable = false)
    private Receipt receipt;

    @Column(nullable = false)
    private Integer lineNumber;

    @Column(nullable = false, length = 150)
    private String itemName;

    @Column(nullable = false)
    private Integer quantity;

    @Column(length = 20)
    private String unit;

    private Integer unitPrice;

    @Column(nullable = false)
    private Integer totalPrice;

    @Column(name = "matched_product_id")
    private Long matchedProductId;

    private ReceiptItem(Integer lineNumber, String itemName, Integer quantity, String unit,
                         Integer unitPrice, Integer totalPrice) {
        this.lineNumber = lineNumber;
        this.itemName = itemName;
        this.quantity = quantity == null ? 1 : quantity;
        this.unit = unit;
        this.unitPrice = unitPrice;
        this.totalPrice = totalPrice;
    }

    public static ReceiptItem create(Integer lineNumber, String itemName, Integer quantity,
                                      String unit, Integer unitPrice, Integer totalPrice) {
        return new ReceiptItem(lineNumber, itemName, quantity, unit, unitPrice, totalPrice);
    }

    void assignReceipt(Receipt receipt) {
        this.receipt = receipt;
    }
}
