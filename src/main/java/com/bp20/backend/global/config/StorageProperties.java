package com.bp20.backend.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 생성된 상품 이미지를 저장할 위치 설정. s3.bucket이 비어있지 않으면 S3를,
 * 비어있으면 local 설정(디스크 경로 + 서빙용 base-url)을 사용한다.
 */
@ConfigurationProperties(prefix = "storage")
public record StorageProperties(Local local, S3 s3) {

    public record Local(String dir, String baseUrl) {
    }

    public record S3(String bucket, String region) {
    }
}
