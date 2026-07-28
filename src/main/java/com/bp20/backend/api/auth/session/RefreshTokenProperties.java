package com.bp20.backend.api.auth.session;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.auth.refresh-token")
public record RefreshTokenProperties(
        String store,
        long expirationSeconds,
        long rememberMeExpirationSeconds,
        String cookieName,
        boolean cookieSecure,
        String cookieSameSite
) {
    public Duration expiration(boolean rememberMe) {
        long seconds = rememberMe ? rememberMeExpirationSeconds : expirationSeconds;
        if (seconds <= 0) {
            throw new IllegalStateException("Refresh Token expiration must be greater than zero.");
        }
        return Duration.ofSeconds(seconds);
    }
}
