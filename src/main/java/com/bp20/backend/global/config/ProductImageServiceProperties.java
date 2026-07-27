package com.bp20.backend.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AI 상품 이미지 생성 Python 서비스의 base-url 설정.
 * application.yml의 product-image-service.base-url 값과 바인딩된다.
 */
@ConfigurationProperties(prefix = "product-image-service")
public record ProductImageServiceProperties(String baseUrl) {
}
