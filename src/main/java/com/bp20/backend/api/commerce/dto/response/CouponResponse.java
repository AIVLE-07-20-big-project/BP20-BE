package com.bp20.backend.api.commerce.dto.response;

import com.bp20.backend.api.commerce.domain.Coupon;
import com.bp20.backend.api.commerce.domain.CouponStatus;
import com.bp20.backend.api.commerce.domain.DiscountType;
import com.bp20.backend.api.commerce.domain.CouponUsageChannel;
import com.bp20.backend.global.util.PersonalDataMasker;

import java.time.LocalDateTime;

public record CouponResponse(
        Long id, String name, CouponStatus status, DiscountType discountType,
        long discountValue, Long customerId, String customerEmail, String customerName,
        CouponUsageChannel usageChannel, Long sourceOnlinePurchaseId,
        LocalDateTime issuedAt, LocalDateTime expiresAt, LocalDateTime usedAt, LocalDateTime revokedAt
) {
    public static CouponResponse from(Coupon coupon) {
        return new CouponResponse(
                coupon.getId(), coupon.getName(), coupon.getStatus(), coupon.getDiscountType(),
                coupon.getDiscountValue(), coupon.getCustomer().getId(),
                PersonalDataMasker.email(coupon.getCustomer().getEmail()),
                PersonalDataMasker.name(coupon.getCustomer().getName()),
                coupon.getUsageChannel(),
                coupon.getSourceOnlinePurchase() == null ? null : coupon.getSourceOnlinePurchase().getId(),
                coupon.getIssuedAt(), coupon.getExpiresAt(), coupon.getUsedAt(), coupon.getRevokedAt()
        );
    }
}
