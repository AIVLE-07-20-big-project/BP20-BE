package com.bp20.backend.api.receipt.client;

import com.bp20.backend.api.receipt.domain.ReceiptItem;

public record ReceiptItemPayload(
        Long receiptItemId,
        Long receiptId,
        String itemName,
        Integer quantity,
        String unit,
        Integer unitPrice,
        Integer totalPrice
) {
    public static ReceiptItemPayload from(ReceiptItem item) {
        return new ReceiptItemPayload(
                item.getId(),
                item.getReceipt().getId(),
                item.getItemName(),
                item.getQuantity(),
                item.getUnit(),
                item.getUnitPrice(),
                item.getTotalPrice()
        );
    }
}
