package com.bp20.backend.global.security.crypto;

import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class AesGcmStringEncryptor {

    private static final String PREFIX = "enc:v1:";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private final SecretKeySpec secretKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public AesGcmStringEncryptor(PersonalDataEncryptionProperties properties) {
        if (properties.encryptionKey() == null || properties.encryptionKey().length() < 32) {
            throw new IllegalStateException("PERSONAL_DATA_ENCRYPTION_KEY must be at least 32 characters.");
        }
        try {
            byte[] key = MessageDigest.getInstance("SHA-256")
                    .digest(properties.encryptionKey().getBytes(StandardCharsets.UTF_8));
            this.secretKey = new SecretKeySpec(key, "AES");
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to initialize personal data encryption.", exception);
        }
    }

    public String encrypt(String plainText) {
        if (plainText == null || plainText.isBlank()) {
            return plainText;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            byte[] payload = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(encrypted, 0, payload, iv.length, encrypted.length);
            return PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(payload);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to encrypt personal data.", exception);
        }
    }

    public String decrypt(String storedValue) {
        if (storedValue == null || storedValue.isBlank() || !storedValue.startsWith(PREFIX)) {
            return storedValue;
        }
        try {
            byte[] payload = Base64.getUrlDecoder().decode(storedValue.substring(PREFIX.length()));
            byte[] iv = new byte[IV_LENGTH];
            byte[] encrypted = new byte[payload.length - IV_LENGTH];
            System.arraycopy(payload, 0, iv, 0, IV_LENGTH);
            System.arraycopy(payload, IV_LENGTH, encrypted, 0, encrypted.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to decrypt personal data.", exception);
        }
    }
}
