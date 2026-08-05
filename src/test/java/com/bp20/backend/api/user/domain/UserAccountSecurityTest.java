package com.bp20.backend.api.user.domain;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class UserAccountSecurityTest {

    @Test
    void locksAccountWhenFailedLoginLimitIsReached() {
        User user = User.createAdmin("admin@bp20.com", "관리자", null, "password-hash");
        LocalDateTime now = LocalDateTime.of(2026, 8, 3, 12, 0);

        for (int attempt = 0; attempt < 5; attempt++) {
            user.registerFailedLogin(5, Duration.ofMinutes(15), now);
        }

        assertThat(user.isTemporarilyLocked(now.plusMinutes(14))).isTrue();
        assertThat(user.isTemporarilyLocked(now.plusMinutes(15))).isFalse();
        assertThat(user.getFailedLoginAttempts()).isZero();
    }

    @Test
    void successfulLoginClearsFailedLoginState() {
        User user = User.createAdmin("admin@bp20.com", "관리자", null, "password-hash");
        LocalDateTime now = LocalDateTime.of(2026, 8, 3, 12, 0);
        user.registerFailedLogin(5, Duration.ofMinutes(15), now);

        user.loginSucceeded();

        assertThat(user.getFailedLoginAttempts()).isZero();
        assertThat(user.getLockedUntil()).isNull();
    }

    @Test
    void passwordExpiresAfterConfiguredMaximumAge() {
        User user = User.createAdmin("admin@bp20.com", "관리자", null, "password-hash");
        LocalDateTime changedAt = LocalDateTime.of(2026, 8, 3, 12, 0);
        user.changePassword("new-password-hash", changedAt);

        assertThat(user.isPasswordExpired(changedAt.plusDays(90), Duration.ofDays(90))).isFalse();
        assertThat(user.isPasswordExpired(changedAt.plusDays(90).plusSeconds(1), Duration.ofDays(90))).isTrue();
    }
}
