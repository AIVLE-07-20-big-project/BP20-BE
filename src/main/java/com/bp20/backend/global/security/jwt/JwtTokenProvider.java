package com.bp20.backend.global.security.jwt;

import com.bp20.backend.api.user.domain.User;
import com.bp20.backend.global.exception.ApiException;
import com.bp20.backend.global.response.ErrorCode;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class JwtTokenProvider {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();

    private final ObjectMapper objectMapper;
    private final byte[] accessTokenSecret;
    private final byte[] refreshTokenSecret;
    private final long expirationSeconds;
    private final long adminExpirationSeconds;

    public JwtTokenProvider(ObjectMapper objectMapper, JwtProperties properties) {
        if (properties.secret() == null
                || properties.secret().getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT_SECRET must be at least 32 bytes.");
        }
        this.objectMapper = objectMapper;
        this.accessTokenSecret = properties.secret().getBytes(StandardCharsets.UTF_8);
        this.refreshTokenSecret = deriveRefreshTokenSecret(accessTokenSecret);
        this.expirationSeconds = properties.expirationSeconds();
        this.adminExpirationSeconds = properties.adminExpirationSeconds();
    }

    public String createAccessToken(User user) {
        Instant now = Instant.now();

        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", "HS256");
        header.put("typ", "JWT");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sub", user.getId().toString());
        payload.put("iat", now.getEpochSecond());
        long tokenExpiration = user.isStoreOwner() ? expirationSeconds : adminExpirationSeconds;
        payload.put("exp", now.plusSeconds(tokenExpiration).getEpochSecond());

        String encodedHeader = encodeJson(header);
        String encodedPayload = encodeJson(payload);
        String signingInput = encodedHeader + "." + encodedPayload;
        String signature = sign(signingInput, accessTokenSecret);

        return signingInput + "." + signature;
    }

    public String createRefreshToken(
            Long userId,
            String sessionId,
            String tokenId,
            Instant expiresAt,
            boolean rememberMe
    ) {
        Instant now = Instant.now();

        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", "HS256");
        header.put("typ", "JWT");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sub", userId.toString());
        payload.put("sid", sessionId);
        payload.put("jti", tokenId);
        payload.put("rmb", rememberMe);
        payload.put("iat", now.getEpochSecond());
        payload.put("exp", expiresAt.getEpochSecond());

        String encodedHeader = encodeJson(header);
        String encodedPayload = encodeJson(payload);
        String signingInput = encodedHeader + "." + encodedPayload;
        String signature = sign(signingInput, refreshTokenSecret);

        return signingInput + "." + signature;
    }

    public Long extractUserId(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new ApiException(ErrorCode.UNAUTHORIZED_INVALID_TOKEN);
        }

        String signingInput = parts[0] + "." + parts[1];
        String expectedSignature = sign(signingInput, accessTokenSecret);
        if (!constantTimeEquals(expectedSignature, parts[2])) {
            throw new ApiException(ErrorCode.UNAUTHORIZED_INVALID_TOKEN);
        }

        Map<String, Object> payload = decodePayload(parts[1]);
        Object expiresAtClaim = payload.get("exp");
        Object subjectClaim = payload.get("sub");
        if (!(expiresAtClaim instanceof Number) || !(subjectClaim instanceof String subject)) {
            throw new ApiException(ErrorCode.UNAUTHORIZED_INVALID_TOKEN);
        }

        long expiresAt = ((Number) expiresAtClaim).longValue();
        if (Instant.now().getEpochSecond() >= expiresAt) {
            throw new ApiException(ErrorCode.UNAUTHORIZED_EXPIRED_TOKEN);
        }

        try {
            return Long.valueOf(subject);
        } catch (NumberFormatException e) {
            throw new ApiException(ErrorCode.UNAUTHORIZED_INVALID_TOKEN);
        }
    }

    public RefreshTokenClaims extractRefreshTokenClaims(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new ApiException(ErrorCode.UNAUTHORIZED_INVALID_REFRESH_TOKEN);
        }

        String signingInput = parts[0] + "." + parts[1];
        String expectedSignature = sign(signingInput, refreshTokenSecret);
        if (!constantTimeEquals(expectedSignature, parts[2])) {
            throw new ApiException(ErrorCode.UNAUTHORIZED_INVALID_REFRESH_TOKEN);
        }

        Map<String, Object> payload = decodePayload(
                parts[1],
                ErrorCode.UNAUTHORIZED_INVALID_REFRESH_TOKEN
        );
        Object expiresAtClaim = payload.get("exp");
        Object subjectClaim = payload.get("sub");
        Object sessionIdClaim = payload.get("sid");
        Object tokenIdClaim = payload.get("jti");
        Object rememberMeClaim = payload.get("rmb");
        if (!(expiresAtClaim instanceof Number)
                || !(subjectClaim instanceof String subject)
                || !(sessionIdClaim instanceof String sessionId)
                || !(tokenIdClaim instanceof String tokenId)
                || !(rememberMeClaim instanceof Boolean rememberMe)) {
            throw new ApiException(ErrorCode.UNAUTHORIZED_INVALID_REFRESH_TOKEN);
        }

        Instant expiresAt = Instant.ofEpochSecond(((Number) expiresAtClaim).longValue());
        if (!expiresAt.isAfter(Instant.now())) {
            throw new ApiException(ErrorCode.UNAUTHORIZED_EXPIRED_REFRESH_TOKEN);
        }

        try {
            return new RefreshTokenClaims(
                    Long.valueOf(subject),
                    sessionId,
                    tokenId,
                    expiresAt,
                    rememberMe
            );
        } catch (NumberFormatException e) {
            throw new ApiException(ErrorCode.UNAUTHORIZED_INVALID_REFRESH_TOKEN);
        }
    }

    private String encodeJson(Map<String, Object> value) {
        try {
            byte[] json = objectMapper.writeValueAsBytes(value);
            return URL_ENCODER.encodeToString(json);
        } catch (Exception e) {
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR, e);
        }
    }

    private Map<String, Object> decodePayload(String encodedPayload) {
        return decodePayload(encodedPayload, ErrorCode.UNAUTHORIZED_INVALID_TOKEN);
    }

    private Map<String, Object> decodePayload(String encodedPayload, ErrorCode errorCode) {
        try {
            byte[] json = URL_DECODER.decode(encodedPayload);
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            throw new ApiException(errorCode);
        }
    }

    private String sign(String signingInput, byte[] signingSecret) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(signingSecret, HMAC_ALGORITHM));
            byte[] signature = mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8));
            return URL_ENCODER.encodeToString(signature);
        } catch (Exception e) {
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR, e);
        }
    }

    private boolean constantTimeEquals(String left, String right) {
        byte[] leftBytes = left.getBytes(StandardCharsets.UTF_8);
        byte[] rightBytes = right.getBytes(StandardCharsets.UTF_8);
        if (leftBytes.length != rightBytes.length) {
            return false;
        }

        int result = 0;
        for (int i = 0; i < leftBytes.length; i++) {
            result |= leftBytes[i] ^ rightBytes[i];
        }
        return result == 0;
    }

    private byte[] deriveRefreshTokenSecret(byte[] sourceSecret) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(sourceSecret, HMAC_ALGORITHM));
            return mac.doFinal("bp20-refresh-token-signing-key".getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to derive Refresh Token signing key.", e);
        }
    }

    public record RefreshTokenClaims(
            Long userId,
            String sessionId,
            String tokenId,
            Instant expiresAt,
            boolean rememberMe
    ) {
    }
}
