package com.bp20.backend.api.receipt.client;

import com.bp20.backend.api.receipt.domain.Receipt;

public record ReceiptPayload(
        Long receiptId,
        Long storeId,
        String vendorName,
        String transactionDate,
        String transactionTime,
        String paymentMethod,
        String category,
        Integer supplyAmount,
        Integer vat,
        Integer taxFreeAmount,
        Integer totalAmount
) {
    public static ReceiptPayload from(Receipt receipt) {
        return new ReceiptPayload(
                receipt.getId(),
                receipt.getStoreId(),
                receipt.getVendorName(),
                receipt.getTransactionDate().toString(),
                receipt.getTransactionTime() == null ? null : receipt.getTransactionTime().toString(),
                receipt.getPaymentMethod(),
                receipt.getCategory(),
                receipt.getSupplyAmount(),
                receipt.getVat(),
                receipt.getTaxFreeAmount(),
                receipt.getTotalAmount()
        );
    }
}
