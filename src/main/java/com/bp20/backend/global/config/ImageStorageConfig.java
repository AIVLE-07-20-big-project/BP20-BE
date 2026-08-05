package com.bp20.backend.global.config;

import com.bp20.backend.global.storage.ImageStorage;
import com.bp20.backend.global.storage.LocalDiskImageStorage;
import com.bp20.backend.global.storage.S3ImageStorage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * product-image-storage.s3.bucket 설정 여부로 상품 이미지 저장소를 고른다.
 * 로컬 디스크를 쓰는 경우를 위해 /product-images/** 정적 서빙도 함께 등록한다
 * (S3를 쓰면 이 핸들러는 그냥 쓰이지 않는다).
 */
@Configuration
public class ImageStorageConfig {

    @Bean
    public ImageStorage imageStorage(
            StorageProperties properties,
            @Value("${AWS_ACCESS_KEY_ID:}") String awsAccessKeyId,
            @Value("${AWS_SECRET_ACCESS_KEY:}") String awsSecretAccessKey
    ) {
        String bucket = properties.s3().bucket();
        if (bucket != null && !bucket.isBlank()) {
            return new S3ImageStorage(properties, awsAccessKeyId, awsSecretAccessKey);
        }
        return new LocalDiskImageStorage(properties);
    }

    @Bean
    public WebMvcConfigurer productImageResourceConfigurer(StorageProperties properties) {
        return new WebMvcConfigurer() {
            @Override
            public void addResourceHandlers(ResourceHandlerRegistry registry) {
                registry.addResourceHandler("/product-images/**")
                        .addResourceLocations("file:" + properties.local().dir() + "/");
            }
        };
    }
}
