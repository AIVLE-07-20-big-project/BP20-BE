package com.bp20.backend.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ocr-service")
public record OcrServiceProperties(String baseUrl) {
}
