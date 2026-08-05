package com.bp20.backend.global.security.crypto;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.personal-data")
public record PersonalDataEncryptionProperties(String encryptionKey) {
}
