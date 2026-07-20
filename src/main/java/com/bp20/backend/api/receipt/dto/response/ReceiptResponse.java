package com.bp20.backend.api.receipt.dto.response;

import com.bp20.backend.api.receipt.domain.Receipt;
import com.bp20.backend.api.receipt.domain.ReceiptItem;
import com.bp20.backend.api.receipt.domain.ReceiptStatus;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record ReceiptResponse(
        Long receiptId,
        Long storeId,
        String documentType,
        String vendorName,
        String businessNumber,
        LocalDate transactionDate,
        LocalTime transactionTime,
        String paymentMethod,
        String category,
        Integer supplyAmount,
        Integer vat,
        Integer taxFreeAmount,
        Integer totalAmount,
        ReceiptStatus status,
        List<ReceiptItemData> items
) {
    public static ReceiptResponse from(Receipt receipt) {
        List<ReceiptItemData> items = receipt.getItems().stream()
                .map(ReceiptResponse::toItemData)
                .toList();

        return new ReceiptResponse(
                receipt.getId(),
                receipt.getStoreId(),
                receipt.getDocumentType(),
                receipt.getVendorName(),
                receipt.getBusinessNumber(),
                receipt.getTransactionDate(),
                receipt.getTransactionTime(),
                receipt.getPaymentMethod(),
                receipt.getCategory(),
                receipt.getSupplyAmount(),
                receipt.getVat(),
                receipt.getTaxFreeAmount(),
                receipt.getTotalAmount(),
                receipt.getStatus(),
                items
        );
    }

    private static ReceiptItemData toItemData(ReceiptItem item) {
        return new ReceiptItemData(
                item.getItemName(), item.getQuantity(), item.getUnit(),
                item.getUnitPrice(), item.getTotalPrice()
        );
    }
}
