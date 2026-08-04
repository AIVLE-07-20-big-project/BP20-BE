package com.bp20.backend.global.security.password;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordPolicyTest {

    private final PasswordPolicy passwordPolicy = new PasswordPolicy();

    @Test
    void acceptsPasswordWithAtLeastThreeCharacterGroups() {
        assertThat(passwordPolicy.isValid("SecurePass123")).isTrue();
        assertThat(passwordPolicy.isValid("secure-pass!23")).isTrue();
    }

    @Test
    void rejectsWeakWhitespaceAndRepeatedPasswords() {
        assertThat(passwordPolicy.isValid("onlylowercase")).isFalse();
        assertThat(passwordPolicy.isValid("Secure Pass123")).isFalse();
        assertThat(passwordPolicy.isValid("SecurePass111!")).isFalse();
    }
}
