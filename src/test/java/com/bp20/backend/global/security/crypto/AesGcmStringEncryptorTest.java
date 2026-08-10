package com.bp20.backend.global.security.crypto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AesGcmStringEncryptorTest {

    private final AesGcmStringEncryptor encryptor = new AesGcmStringEncryptor(
            new PersonalDataEncryptionProperties("test-personal-data-key-with-more-than-32-characters")
    );

    @Test
    void encryptsWithRandomIvAndDecryptsOriginalValue() {
        String first = encryptor.encrypt("홍길동");
        String second = encryptor.encrypt("홍길동");

        assertThat(first).startsWith("enc:v1:").isNotEqualTo(second);
        assertThat(encryptor.decrypt(first)).isEqualTo("홍길동");
        assertThat(encryptor.decrypt(second)).isEqualTo("홍길동");
    }

    @Test
    void readsLegacyPlainTextForGradualMigration() {
        assertThat(encryptor.decrypt("legacy-name")).isEqualTo("legacy-name");
    }
}
