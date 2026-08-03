package com.bp20.backend.api.auth.session;

import java.time.Duration;

public interface RefreshTokenStore {

    void save(String sessionId, Long userId, String tokenId, Duration ttl);

    RotationResult rotate(
            String sessionId,
            Long userId,
            String currentTokenId,
            String nextTokenId,
            Duration ttl
    );

    void revoke(String sessionId, String tokenId, Duration ttl);

    void deleteSession(String sessionId);

    enum RotationResult {
        ROTATED,
        REUSED,
        INVALID
    }
}
