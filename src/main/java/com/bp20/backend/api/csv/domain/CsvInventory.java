package com.bp20.backend.api.csv.domain;

import com.bp20.backend.api.recommendation.dto.InventoryDataRequest;
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
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "csv_inventories", uniqueConstraints = @UniqueConstraint(
        name = "uk_csv_inventory_owner_lot", columnNames = {"owner_id", "lot"}
))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CsvInventory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;
    @Column(nullable = false, length = 100) private String name;
    @Column(nullable = false, length = 60) private String lot;
    @Column(nullable = false) private double stock;
    @Column(nullable = false, length = 30) private String unit;
    @Column(name = "expected_depletion", nullable = false, length = 50) private String expectedDepletion;
    @Column(nullable = false, length = 50) private String expiry;
    @Column(nullable = false, length = 100) private String supplier;
    @Column(nullable = false, length = 20) private String status;
    @Column(name = "reorder_qty", nullable = false) private long reorderQty;
    @Column(name = "supplier_price", nullable = false) private long supplierPrice;
    @Column(name = "lead_time", nullable = false) private int leadTime;

    public CsvInventory(User owner, InventoryDataRequest value) {
        this.owner = owner;
        this.name = value.name();
        this.lot = value.lot();
        this.stock = value.stock();
        this.unit = value.unit();
        this.expectedDepletion = value.expectedDepletion();
        this.expiry = value.expiry();
        this.supplier = value.supplier();
        this.status = value.status();
        this.reorderQty = value.reorderQty();
        this.supplierPrice = value.supplierPrice();
        this.leadTime = value.leadTime();
    }

    public InventoryDataRequest toDto() {
        return new InventoryDataRequest(name, lot, stock, unit, expectedDepletion, expiry, supplier, status, reorderQty, supplierPrice, leadTime);
    }
}
