package com.bp20.backend.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 생성된 상품 이미지를 저장할 위치 설정. s3.bucket이 비어있지 않으면 S3를,
 * 비어있으면 local 설정(디스크 경로 + 서빙용 base-url)을 사용한다.
 *
 * prefix가 "product-image-storage"인 이유: main과 merge하면서 main이 추가한
 * 범용 오브젝트 스토리지(S3ObjectStorageService, AiService의 CSV 업로드용)도
 * 같은 "storage.s3.bucket/region" 경로를 쓰고 있었다. 두 기능이 같은 YAML 경로를
 * 공유하면 버킷/리전 설정이 서로 덮어써지므로, 상품 이미지 전용 네임스페이스로 분리했다.
 */
@ConfigurationProperties(prefix = "product-image-storage")
public record StorageProperties(Local local, S3 s3) {

    public record Local(String dir, String baseUrl) {
    }

    public record S3(String bucket, String region) {
    }
}
