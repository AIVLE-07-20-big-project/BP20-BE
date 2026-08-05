package com.bp20.backend.api.receipt.dto.request;

import com.bp20.backend.api.receipt.dto.response.ReceiptItemData;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 업로드 내역에서 영수증을 직접 수정할 때 보내는 요청. ReceiptCreateRequest와 필드 구성은 같되
 * storeId/uploadedByUserId/force처럼 최초 저장에만 필요한 값은 뺐다.
 */
public record ReceiptUpdateRequest(

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
        String category
) {
}
