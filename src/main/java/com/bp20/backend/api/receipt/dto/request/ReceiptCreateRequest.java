package com.bp20.backend.api.receipt.dto.request;

import com.bp20.backend.api.receipt.dto.response.ReceiptItemData;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * OCR 파싱 결과(ReceiptParseResult)를 사용자가 검토/수정한 뒤 최종 저장을 확정할 때 보내는 요청.
 * 필드 구성은 ReceiptParseResult와 동일하되, 저장에 필요한 storeId가 추가된다.
 */
public record ReceiptCreateRequest(

        @NotNull(message = "storeId는 필수입니다.")
        Long storeId,

        Long uploadedByUserId,

        @NotBlank(message = "documentType은 필수입니다.")
        String documentType,

        String storeName,

        String businessNumber,

        @NotBlank(message = "transactionDate는 필수입니다.")
        String transactionDate,

        String transactionTime,

        @NotBlank(message = "paymentMethod는 필수입니다.")
        String paymentMethod,

        @Valid
        List<ReceiptItemData> items,

        Integer supplyAmount,

        Integer vat,

        Integer taxFreeAmount,

        @NotNull(message = "totalAmount는 필수입니다.")
        Integer totalAmount,

        @NotBlank(message = "category는 필수입니다.")
        String category,

        String rawImagePath,

        /** 중복 의심이어도 강제로 저장할지 여부 (기본 false - 중복이면 409로 안내). 필드 자체를 안 보내도 false로 처리된다. */
        Boolean force
) {
    public ReceiptCreateRequest {
        if (force == null) {
            force = false;
        }
    }
}