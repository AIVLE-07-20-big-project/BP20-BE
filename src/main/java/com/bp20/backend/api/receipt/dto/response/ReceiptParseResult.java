package com.bp20.backend.api.receipt.dto.response;

import java.util.List;

/**
 * Python 마이크로서비스(POST /api/v1/receipts/parse)가 돌려주는 "result" 객체와 1:1 대응.
 * 필드명은 Python 쪽 camelCase JSON과 그대로 맞춰서 Jackson이 자동 매핑하도록 한다.
 */
public record ReceiptParseResult(
        String documentType,
        String storeName,
        String businessNumber,
        String transactionDate,   // "YYYY-MM-DD" - 저장 시 LocalDate.parse
        String transactionTime,   // "HH:mm" - 저장 시 LocalTime.parse (null 가능)
        String paymentMethod,
        List<ReceiptItemData> items,
        Integer supplyAmount,
        Integer vat,
        Integer taxFreeAmount,
        Integer totalAmount,
        String category
) {
}
