package com.bp20.backend.api.receipt.client;

import com.bp20.backend.api.product.domain.Product;

public record ProductPayload(
        Long productId,
        Long storeId,
        String productName,
        String category,
        Long price,
        Integer discountRate
) {
    /**
     * discountRate는 더 이상 Product에 저장된 값이 아니라, 호출 측(ReceiptAnalyticsService)이
     * Discount 테이블에서 조회한 "현재 활성 할인율"을 넘겨준다(PR #35 리뷰 코멘트 - MenuItem 폐기).
     * 활성 할인이 없으면 0을 넘긴다.
     */
    public static ProductPayload from(Product product, int discountRatePercent) {
        return new ProductPayload(
                product.getId(),
                product.getStore().getId(),
                product.getName(),
                product.getCategory(),
                product.getPrice(),
                discountRatePercent
        );
    }
}
