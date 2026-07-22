package com.bp20.backend.global.security.jwt;

import com.bp20.backend.global.exception.ApiException;
import com.bp20.backend.global.response.ErrorCode;
import org.springframework.stereotype.Component;

@Component
public class BearerTokenResolver {

    private static final String BEARER_PREFIX = "Bearer ";

    public String resolve(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            throw new ApiException(ErrorCode.UNAUTHORIZED_TOKEN_EMPTY);
        }

        if (!authorizationHeader.startsWith(BEARER_PREFIX)) {
            throw new ApiException(ErrorCode.UNAUTHORIZED_INVALID_TOKEN);
        }

        String token = authorizationHeader.substring(BEARER_PREFIX.length()).trim();
        if (token.isBlank()) {
            throw new ApiException(ErrorCode.UNAUTHORIZED_TOKEN_EMPTY);
        }

        return token;
    }
}
