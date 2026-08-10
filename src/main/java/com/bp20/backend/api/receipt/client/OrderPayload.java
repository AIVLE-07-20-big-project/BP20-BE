package com.bp20.backend.api.receipt.client;

import com.bp20.backend.api.order.domain.Order;

public record OrderPayload(
        Long orderId,
        Long storeId,
        String orderType,
        Long totalAmount,
        Long discountAmount,
        String paymentMethod,
        String orderedDate,
        String orderedTime
) {
    public static OrderPayload from(Order order) {
        return new OrderPayload(
                order.getId(),
                order.getStore().getId(),
                order.getOrderType(),
                order.getTotalAmount(),
                order.getDiscountAmount(),
                order.getPaymentMethod(),
                order.getOrderedDate().toString(),
                order.getOrderedTime() == null ? null : order.getOrderedTime().toString()
        );
    }
}
