package com.bp20.backend.api.auth.session;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ConditionalOnProperty(
        prefix = "app.auth.refresh-token",
        name = "store",
        havingValue = "memory"
)
public class InMemoryRefreshTokenStore implements RefreshTokenStore {

    private final Map<String, ExpiringValue> sessions = new ConcurrentHashMap<>();
    private final Map<String, ExpiringValue> usedTokens = new ConcurrentHashMap<>();

    @Override
    public void save(String sessionId, Long userId, String tokenId, Duration ttl) {
        sessions.put(sessionId, value(sessionValue(userId, tokenId), ttl));
    }

    @Override
    public synchronized RotationResult rotate(
            String sessionId,
            Long userId,
            String currentTokenId,
            String nextTokenId,
            Duration ttl
    ) {
        removeExpired();
        if (usedTokens.containsKey(currentTokenId)) {
            return RotationResult.REUSED;
        }

        ExpiringValue current = sessions.get(sessionId);
        if (current == null || !current.value().equals(sessionValue(userId, currentTokenId))) {
            return RotationResult.INVALID;
        }

        sessions.put(sessionId, value(sessionValue(userId, nextTokenId), ttl));
        usedTokens.put(currentTokenId, value(sessionId, ttl));
        return RotationResult.ROTATED;
    }

    @Override
    public synchronized void revoke(String sessionId, String tokenId, Duration ttl) {
        sessions.remove(sessionId);
        usedTokens.put(tokenId, value(sessionId, ttl));
    }

    @Override
    public void deleteSession(String sessionId) {
        sessions.remove(sessionId);
    }

    private void removeExpired() {
        Instant now = Instant.now();
        sessions.entrySet().removeIf(entry -> !entry.getValue().expiresAt().isAfter(now));
        usedTokens.entrySet().removeIf(entry -> !entry.getValue().expiresAt().isAfter(now));
    }

    private ExpiringValue value(String value, Duration ttl) {
        Duration normalizedTtl = ttl.isNegative() || ttl.isZero() ? Duration.ofSeconds(1) : ttl;
        return new ExpiringValue(value, Instant.now().plus(normalizedTtl));
    }

    private String sessionValue(Long userId, String tokenId) {
        return userId + ":" + tokenId;
    }

    private record ExpiringValue(String value, Instant expiresAt) {
    }
}
