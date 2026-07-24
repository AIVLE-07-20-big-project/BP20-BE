package com.bp20.backend.api.commerce.dto.response;

import com.bp20.backend.api.commerce.domain.CouponStatus;
import com.bp20.backend.api.commerce.domain.CustomerCoupon;
import com.bp20.backend.api.commerce.domain.DiscountType;

import java.time.LocalDateTime;

public record CustomerCouponResponse(
        Long id,
        String name,
        String code,
        CouponStatus status,
        DiscountType discountType,
        long discountValue,
        Long customerId,
        String customerEmail,
        String customerName,
        LocalDateTime issuedAt,
        LocalDateTime expiresAt,
        LocalDateTime usedAt,
        LocalDateTime revokedAt
) {
    public static CustomerCouponResponse from(CustomerCoupon coupon) {
        return new CustomerCouponResponse(
                coupon.getId(),
                coupon.getName(),
                coupon.getCode(),
                coupon.getStatus(),
                coupon.getDiscountType(),
                coupon.getDiscountValue(),
                coupon.getCustomer().getId(),
                coupon.getCustomer().getEmail(),
                coupon.getCustomer().getName(),
                coupon.getIssuedAt(),
                coupon.getExpiresAt(),
                coupon.getUsedAt(),
                coupon.getRevokedAt()
        );
    }
}
