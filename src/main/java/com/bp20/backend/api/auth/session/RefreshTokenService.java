package com.bp20.backend.api.auth.session;

import com.bp20.backend.api.user.domain.User;
import com.bp20.backend.api.user.repository.UserRepository;
import com.bp20.backend.global.exception.ApiException;
import com.bp20.backend.global.response.ErrorCode;
import com.bp20.backend.global.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenStore refreshTokenStore;
    private final RefreshTokenProperties properties;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    public TokenPair issue(User user, boolean rememberMe) {
        Duration ttl = properties.expiration(rememberMe);
        Instant expiresAt = Instant.now().plus(ttl);
        String sessionId = UUID.randomUUID().toString();
        String tokenId = UUID.randomUUID().toString();
        String refreshToken = jwtTokenProvider.createRefreshToken(
                user.getId(),
                sessionId,
                tokenId,
                expiresAt,
                rememberMe
        );

        refreshTokenStore.save(sessionId, user.getId(), tokenId, ttl);
        return new TokenPair(
                jwtTokenProvider.createAccessToken(user),
                refreshToken,
                ttl,
                rememberMe
        );
    }

    public TokenPair rotate(String refreshToken) {
        JwtTokenProvider.RefreshTokenClaims claims =
                jwtTokenProvider.extractRefreshTokenClaims(refreshToken);
        Duration remainingTtl = Duration.between(Instant.now(), claims.expiresAt());
        User user = userRepository.findById(claims.userId())
                .filter(User::isActive)
                .orElseThrow(() -> {
                    refreshTokenStore.deleteSession(claims.sessionId());
                    return new ApiException(ErrorCode.UNAUTHORIZED_INVALID_REFRESH_TOKEN);
                });

        String nextTokenId = UUID.randomUUID().toString();
        RefreshTokenStore.RotationResult rotationResult = refreshTokenStore.rotate(
                claims.sessionId(),
                claims.userId(),
                claims.tokenId(),
                nextTokenId,
                remainingTtl
        );

        if (rotationResult == RefreshTokenStore.RotationResult.REUSED) {
            refreshTokenStore.deleteSession(claims.sessionId());
            throw new ApiException(ErrorCode.UNAUTHORIZED_REFRESH_TOKEN_REUSED);
        }
        if (rotationResult == RefreshTokenStore.RotationResult.INVALID) {
            throw new ApiException(ErrorCode.UNAUTHORIZED_INVALID_REFRESH_TOKEN);
        }

        String nextRefreshToken = jwtTokenProvider.createRefreshToken(
                user.getId(),
                claims.sessionId(),
                nextTokenId,
                claims.expiresAt(),
                claims.rememberMe()
        );
        return new TokenPair(
                jwtTokenProvider.createAccessToken(user),
                nextRefreshToken,
                remainingTtl,
                claims.rememberMe()
        );
    }

    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }

        try {
            JwtTokenProvider.RefreshTokenClaims claims =
                    jwtTokenProvider.extractRefreshTokenClaims(refreshToken);
            refreshTokenStore.revoke(
                    claims.sessionId(),
                    claims.tokenId(),
                    Duration.between(Instant.now(), claims.expiresAt())
            );
        } catch (ApiException ignored) {
            // 로그아웃은 멱등성을 보장하며, 잘못되거나 만료된 쿠키도 클라이언트에서 제거합니다.
        }
    }

    public record TokenPair(
            String accessToken,
            String refreshToken,
            Duration refreshTokenTtl,
            boolean rememberMe
    ) {
    }
}
