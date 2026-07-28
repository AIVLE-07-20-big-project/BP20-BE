package com.bp20.backend.api.productimage.service;

import com.bp20.backend.api.productimage.client.ProductImageServiceClient;
import com.bp20.backend.api.productimage.dto.response.CategoriesResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * AI 상품 이미지 생성 기능: 실제 이미지 처리(배경 제거·합성)는 Python 서비스에 위임하고,
 * 이 서비스는 필요 시 향후 DB 연동(생성 이력 저장 등)을 위한 자리를 겸한다.
 * 현재는 상태 없이 클라이언트 호출만 그대로 전달(pass-through)한다.
 */
@Service
@RequiredArgsConstructor
public class ProductImageService {

    private final ProductImageServiceClient productImageServiceClient;

    public CategoriesResponse getCategories() {
        return productImageServiceClient.getCategories();
    }

    public byte[] generateProductImage(MultipartFile file, String category) {
        return productImageServiceClient.generateProductImage(file, category);
        // TODO: 생성 이력을 저장할 도메인(예: ProductImageHistory)이 추가되면
        //       여기서 storeId/생성 결과를 함께 저장하도록 확장
    }
}
