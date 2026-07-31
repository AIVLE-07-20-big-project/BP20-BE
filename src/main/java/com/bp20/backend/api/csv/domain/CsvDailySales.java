package com.bp20.backend.api.csv.domain;

import com.bp20.backend.api.recommendation.dto.DailySalesDto;
import com.bp20.backend.api.user.domain.User;
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

import java.time.LocalDate;

@Entity
@Table(name = "csv_daily_sales")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CsvDailySales {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;
    @Column(name = "sale_date", nullable = false)
    private LocalDate saleDate;
    @Column(name = "product_code", nullable = false, length = 50)
    private String productCode;

    @Column(name = "product_name", nullable = false, length = 100)
    private String productName;
    @Column(name = "sales_quantity", nullable = false)
    private long salesQuantity;
    @Column(name = "unit_price", nullable = false)
    private long unitPrice;
    @Column(name = "sales_amount", nullable = false)
    private long salesAmount;

    public CsvDailySales(User owner, DailySalesDto value) {
        this.owner = owner;
        this.saleDate = value.saleDate();
        this.productCode = value.productCode();
        this.productName = value.productName();
        this.salesQuantity = value.salesQuantity();
        this.unitPrice = value.unitPrice();
        this.salesAmount = value.salesAmount();
    }

    public DailySalesDto toDto() {
        return new DailySalesDto(saleDate, productCode, productName, salesQuantity, unitPrice, salesAmount);
    }
}
