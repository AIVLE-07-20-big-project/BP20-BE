package com.bp20.backend.api.receipt.dto.request;

import com.bp20.backend.api.receipt.dto.response.ReceiptItemData;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ReceiptUpdateRequest(
        @NotBlank String documentType,
        String storeName,
        String businessNumber,
        @NotBlank String transactionDate,
        String transactionTime,
        @NotBlank String paymentMethod,
        @Valid List<ReceiptItemData> items,
        Integer supplyAmount,
        Integer vat,
        Integer taxFreeAmount,
        @NotNull Integer totalAmount,
        @NotBlank String category
) {
}
