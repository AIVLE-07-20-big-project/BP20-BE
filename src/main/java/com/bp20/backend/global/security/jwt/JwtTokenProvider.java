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
    private final byte[] secret;
    private final long expirationSeconds;
    private final long adminExpirationSeconds;

    public JwtTokenProvider(ObjectMapper objectMapper, JwtProperties properties) {
        if (properties.secret() == null
                || properties.secret().getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT_SECRET must be at least 32 bytes.");
        }
        this.objectMapper = objectMapper;
        this.secret = properties.secret().getBytes(StandardCharsets.UTF_8);
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
        String signature = sign(signingInput);

        return signingInput + "." + signature;
    }

    public Long extractUserId(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new ApiException(ErrorCode.UNAUTHORIZED_INVALID_TOKEN);
        }

        String signingInput = parts[0] + "." + parts[1];
        String expectedSignature = sign(signingInput);
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

    private String encodeJson(Map<String, Object> value) {
        try {
            byte[] json = objectMapper.writeValueAsBytes(value);
            return URL_ENCODER.encodeToString(json);
        } catch (Exception e) {
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR, e);
        }
    }

    private Map<String, Object> decodePayload(String encodedPayload) {
        try {
            byte[] json = URL_DECODER.decode(encodedPayload);
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            throw new ApiException(ErrorCode.UNAUTHORIZED_INVALID_TOKEN);
        }
    }

    private String sign(String signingInput) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
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
}
