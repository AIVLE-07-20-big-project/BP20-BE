package com.bp20.backend.api.commerce.dto.response;

import com.bp20.backend.api.commerce.domain.Discount;
import com.bp20.backend.api.commerce.domain.DiscountStatus;
import com.bp20.backend.api.commerce.domain.DiscountType;

import java.time.LocalDateTime;
import java.time.LocalTime;

public record DiscountResponse(
        Long id,
        String name,
        String description,
        DiscountType discountType,
        long discountValue,
        DiscountProductResponse product,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        LocalTime dailyStartTime,
        LocalTime dailyEndTime,
        boolean reminderEnabled,
        DiscountStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static DiscountResponse from(Discount discount) {
        return new DiscountResponse(
                discount.getId(),
                discount.getName(),
                discount.getDescription(),
                discount.getDiscountType(),
                discount.getDiscountValue(),
                DiscountProductResponse.from(discount.getProduct()),
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
