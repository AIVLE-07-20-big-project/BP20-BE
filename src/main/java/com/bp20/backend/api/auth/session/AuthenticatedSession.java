package com.bp20.backend.api.auth.session;

import java.time.Duration;

public record AuthenticatedSession<T>(
        T response,
        String refreshToken,
        Duration refreshTokenTtl,
        boolean rememberMe
) {
    public static <T> AuthenticatedSession<T> of(
            T response,
            RefreshTokenService.TokenPair tokenPair
    ) {
        return new AuthenticatedSession<>(
                response,
                tokenPair.refreshToken(),
                tokenPair.refreshTokenTtl(),
                tokenPair.rememberMe()
        );
    }
}
