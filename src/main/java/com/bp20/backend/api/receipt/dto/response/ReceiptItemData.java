package com.bp20.backend.api.receipt.dto.response;

public record ReceiptItemData(
        String itemName,
        Integer quantity,
        String unit,
        Integer unitPrice,
        Integer totalPrice
) {
}
