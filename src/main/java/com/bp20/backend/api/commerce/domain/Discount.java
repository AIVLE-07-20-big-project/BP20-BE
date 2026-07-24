package com.bp20.backend.api.commerce.domain;

import com.bp20.backend.api.product.domain.Product;
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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Entity
@Table(
        name = "discounts",
        indexes = {
                @Index(name = "idx_discounts_store_status", columnList = "store_id,status"),
                @Index(name = "idx_discounts_period", columnList = "starts_at,ends_at")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Discount extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "discount_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 20)
    private DiscountType discountType;

    @Column(name = "discount_value", nullable = false)
    private long discountValue;

    @Column(name = "starts_at", nullable = false)
    private LocalDateTime startsAt;

    @Column(name = "ends_at", nullable = false)
    private LocalDateTime endsAt;

    @Column(name = "daily_start_time")
    private LocalTime dailyStartTime;

    @Column(name = "daily_end_time")
    private LocalTime dailyEndTime;

    @Column(name = "reminder_enabled", nullable = false)
    private boolean reminderEnabled;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DiscountStatus status;

    private Discount(
            Store store,
            Product product,
            String name,
            String description,
            DiscountType discountType,
            long discountValue,
            LocalDateTime startsAt,
            LocalDateTime endsAt,
            LocalTime dailyStartTime,
            LocalTime dailyEndTime,
            boolean reminderEnabled
    ) {
        this.store = store;
        this.product = product;
        this.name = name;
        this.description = description;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.dailyStartTime = dailyStartTime;
        this.dailyEndTime = dailyEndTime;
        this.reminderEnabled = reminderEnabled;
        this.status = DiscountStatus.DRAFT;
    }

    public static Discount create(
            Store store,
            Product product,
            String name,
            String description,
            DiscountType discountType,
            long discountValue,
            LocalDateTime startsAt,
            LocalDateTime endsAt,
            LocalTime dailyStartTime,
            LocalTime dailyEndTime,
            boolean reminderEnabled
    ) {
        return new Discount(
                store,
                product,
                name,
                description,
                discountType,
                discountValue,
                startsAt,
                endsAt,
                dailyStartTime,
                dailyEndTime,
                reminderEnabled
        );
    }

    public void changeStatus(DiscountStatus status) {
        this.status = status;
    }
}
