package com.bp20.backend.api.commerce.dto.response;

import com.bp20.backend.api.commerce.domain.DiscountStatus;
import com.bp20.backend.api.commerce.domain.DiscountType;
import com.bp20.backend.api.commerce.domain.OnlineDiscount;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public record OnlineDiscountResponse(
        Long id,
        String name,
        String description,
        DiscountType discountType,
        long discountValue,
        List<OnlineDiscountProductResponse> products,
        List<OnlineDiscountBundleResponse> bundles,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        LocalTime dailyStartTime,
        LocalTime dailyEndTime,
        boolean reminderEnabled,
        DiscountStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static OnlineDiscountResponse from(OnlineDiscount discount) {
        return new OnlineDiscountResponse(
                discount.getId(),
                discount.getName(),
                discount.getDescription(),
                discount.getDiscountType(),
                discount.getDiscountValue(),
                discount.getDiscountProducts().stream()
                        .map(link -> OnlineDiscountProductResponse.from(link.getProduct()))
                        .toList(),
                discount.getDiscountBundles().stream()
                        .map(link -> OnlineDiscountBundleResponse.from(link.getBundle()))
                        .toList(),
                discount.getStartsAt(),
                discount.getEndsAt(),
                discount.getDailyStartTime(),
                discount.getDailyEndTime(),
                discount.isReminderEnabled(),
                discount.getStatus(),
                discount.getCreatedAt(),
                discount.getUpdatedAt()
        );
    }
}
