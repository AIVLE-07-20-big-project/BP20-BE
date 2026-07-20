package com.bp20.backend.api.iam.invitation.service;

import com.bp20.backend.api.iam.invitation.service.TemporaryPasswordService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TemporaryPasswordServiceTest {

    private final TemporaryPasswordService temporaryPasswordService = new TemporaryPasswordService();

    @Test
    void generatesSixteenCharacterPasswordWithEveryRequiredCharacterGroup() {
        for (int attempt = 0; attempt < 100; attempt++) {
            String temporaryPassword = temporaryPasswordService.generate();

            assertThat(temporaryPassword).hasSize(16);
            assertThat(temporaryPassword).containsPattern("[A-Z]");
            assertThat(temporaryPassword).containsPattern("[a-z]");
            assertThat(temporaryPassword).containsPattern("[0-9]");
            assertThat(temporaryPassword).containsPattern("[!@#$%]");
        }
    }
}
