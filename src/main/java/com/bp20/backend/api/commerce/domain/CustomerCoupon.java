package com.bp20.backend.api.commerce.domain;

import com.bp20.backend.api.customer.domain.Customer;
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

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "customer_coupons",
        uniqueConstraints = @UniqueConstraint(name = "uk_customer_coupons_code", columnNames = "code"),
        indexes = {
                @Index(name = "idx_customer_coupons_store_status", columnList = "store_id,status"),
                @Index(name = "idx_customer_coupons_customer_status", columnList = "customer_id,status")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CustomerCoupon extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "customer_coupon_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(nullable = false, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 20)
    private DiscountType discountType;

    @Column(name = "discount_value", nullable = false)
    private long discountValue;

    @Column(nullable = false, length = 32)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CouponStatus status;

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    private CustomerCoupon(
            Store store,
            Customer customer,
            String name,
            DiscountType discountType,
            long discountValue,
            String code,
            LocalDateTime issuedAt,
            LocalDateTime expiresAt
    ) {
        this.store = store;
        this.customer = customer;
        this.name = name;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.code = code;
        this.status = CouponStatus.ISSUED;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
    }

    public static CustomerCoupon issue(
            Store store,
            Customer customer,
            String name,
            DiscountType discountType,
            long discountValue,
            String code,
            LocalDateTime issuedAt,
            LocalDateTime expiresAt
    ) {
        return new CustomerCoupon(
                store,
                customer,
                name,
                discountType,
                discountValue,
                code,
                issuedAt,
                expiresAt
        );
    }

    public boolean isUsable(LocalDateTime now) {
        return status == CouponStatus.ISSUED && now.isBefore(expiresAt);
    }

    public void use(LocalDateTime now) {
        this.status = CouponStatus.USED;
        this.usedAt = now;
    }

    public void revoke(LocalDateTime now) {
        this.status = CouponStatus.REVOKED;
        this.revokedAt = now;
    }

    public void expire() {
        this.status = CouponStatus.EXPIRED;
    }
}
