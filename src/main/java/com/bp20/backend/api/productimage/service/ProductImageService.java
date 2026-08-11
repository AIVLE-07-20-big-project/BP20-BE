package com.bp20.backend.api.productimage.service;

import com.bp20.backend.api.productimage.client.ProductImageServiceClient;
import com.bp20.backend.api.productimage.dto.response.CategoriesResponse;
import com.bp20.backend.api.productimage.dto.response.ProductImageResponse;
import com.bp20.backend.global.storage.ImageStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * AI 상품 이미지 생성 기능: 실제 이미지 처리(배경 제거·합성)는 Python 서비스에 위임하고,
 * 생성된 이미지는 이 서비스가 저장소(ImageStorage - 로컬 디스크 또는 S3)에 저장해 URL로 돌려준다.
 * 호출 1건당 실제 비용(OpenAI API 과금)이 발생하므로, 저장해두면 같은 이미지를 다시 만들 필요가 없다.
 * 어떤 상품에 적용할지는 프론트에서 반환된 imageUrl로 기존 상품 수정 API를 호출해서 정한다.
 */
@Service
@RequiredArgsConstructor
public class ProductImageService {

    private final ProductImageServiceClient productImageServiceClient;
    private final ImageStorage imageStorage;

    public CategoriesResponse getCategories() {
        return productImageServiceClient.getCategories();
    }

    public ProductImageResponse generateAndStore(MultipartFile file, String category, String prompt) {
        byte[] imageBytes = productImageServiceClient.generateProductImage(file, category, prompt);
        String filename = UUID.randomUUID() + ".png";
        String imageUrl = imageStorage.store(imageBytes, filename);
        return new ProductImageResponse(imageUrl);
    }
}
