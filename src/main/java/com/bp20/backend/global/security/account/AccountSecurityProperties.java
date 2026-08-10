package com.bp20.backend.global.security.account;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.auth.account-security")
public record AccountSecurityProperties(
        int passwordMaxAgeDays,
        int maxFailedLoginAttempts,
        int accountLockMinutes
) {
    public AccountSecurityProperties {
        passwordMaxAgeDays = passwordMaxAgeDays > 0 ? passwordMaxAgeDays : 90;
        maxFailedLoginAttempts = maxFailedLoginAttempts > 0 ? maxFailedLoginAttempts : 5;
        accountLockMinutes = accountLockMinutes > 0 ? accountLockMinutes : 15;
    }

    public Duration passwordMaxAge() {
        return Duration.ofDays(passwordMaxAgeDays);
    }

    public Duration accountLockDuration() {
        return Duration.ofMinutes(accountLockMinutes);
    }
}
