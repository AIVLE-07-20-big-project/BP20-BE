package com.bp20.backend.api.auth.session;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Component
@ConditionalOnProperty(
        prefix = "app.auth.refresh-token",
        name = "store",
        havingValue = "redis",
        matchIfMissing = true
)
public class RedisRefreshTokenStore implements RefreshTokenStore {

    private static final String SESSION_KEY_PREFIX = "auth:refresh:session:";
    private static final String USED_KEY_PREFIX = "auth:refresh:used:";

    private static final DefaultRedisScript<Long> ROTATE_SCRIPT = new DefaultRedisScript<>(
            """
            if redis.call('EXISTS', KEYS[2]) == 1 then
                return -1
            end
            local current = redis.call('GET', KEYS[1])
            if not current or current ~= ARGV[1] then
                return 0
            end
            redis.call('SET', KEYS[1], ARGV[2], 'EX', ARGV[3])
            redis.call('SET', KEYS[2], ARGV[4], 'EX', ARGV[3])
            return 1
            """,
            Long.class
    );

    private static final DefaultRedisScript<Long> REVOKE_SCRIPT = new DefaultRedisScript<>(
            """
            redis.call('DEL', KEYS[1])
            redis.call('SET', KEYS[2], ARGV[1], 'EX', ARGV[2])
            return 1
            """,
            Long.class
    );

    private final StringRedisTemplate redisTemplate;

    public RedisRefreshTokenStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void save(String sessionId, Long userId, String tokenId, Duration ttl) {
        redisTemplate.opsForValue().set(
                sessionKey(sessionId),
                sessionValue(userId, tokenId),
                normalizedTtl(ttl)
        );
    }

    @Override
    public RotationResult rotate(
            String sessionId,
            Long userId,
            String currentTokenId,
            String nextTokenId,
            Duration ttl
    ) {
        long ttlSeconds = normalizedTtl(ttl).toSeconds();
        Long result = redisTemplate.execute(
                ROTATE_SCRIPT,
                List.of(sessionKey(sessionId), usedKey(currentTokenId)),
                sessionValue(userId, currentTokenId),
                sessionValue(userId, nextTokenId),
                Long.toString(ttlSeconds),
                sessionId
        );

        if (result == null || result == 0L) {
            return RotationResult.INVALID;
        }
        return result == -1L ? RotationResult.REUSED : RotationResult.ROTATED;
    }

    @Override
    public void revoke(String sessionId, String tokenId, Duration ttl) {
        redisTemplate.execute(
                REVOKE_SCRIPT,
                List.of(sessionKey(sessionId), usedKey(tokenId)),
                sessionId,
                Long.toString(normalizedTtl(ttl).toSeconds())
        );
    }

    @Override
    public void deleteSession(String sessionId) {
        redisTemplate.delete(sessionKey(sessionId));
    }

    private Duration normalizedTtl(Duration ttl) {
        return ttl.isNegative() || ttl.isZero() ? Duration.ofSeconds(1) : ttl;
    }

    private String sessionValue(Long userId, String tokenId) {
        return userId + ":" + tokenId;
    }

    private String sessionKey(String sessionId) {
        return SESSION_KEY_PREFIX + sessionId;
    }

    private String usedKey(String tokenId) {
        return USED_KEY_PREFIX + tokenId;
    }
}
