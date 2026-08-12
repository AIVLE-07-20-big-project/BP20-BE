package com.bp20.backend.global.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class S3ClientConfig {
    @Bean
    S3Client s3Client(@Value("${storage.s3.region:ap-northeast-2}") String region) {
        return S3Client.builder()
                .region(Region.of(region))
                .crossRegionAccessEnabled(true)
                .build();
    }
}
