package com.bp20.backend.api.auth.session;

import com.bp20.backend.global.exception.ApiException;
import com.bp20.backend.global.response.ErrorCode;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

@Component
public class RefreshTokenCookieManager {

    private static final String COOKIE_PATH = "/api/auth";

    private final RefreshTokenProperties properties;

    public RefreshTokenCookieManager(RefreshTokenProperties properties) {
        this.properties = properties;
    }

    public String require(HttpServletRequest request) {
        return read(request)
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED_REFRESH_TOKEN_REQUIRED));
    }

    public Optional<String> read(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        return Arrays.stream(cookies)
                .filter(cookie -> properties.cookieName().equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(value -> !value.isBlank())
                .findFirst();
    }

    public void write(
            HttpServletResponse response,
            String refreshToken,
            Duration ttl,
            boolean rememberMe
    ) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie
                .from(properties.cookieName(), refreshToken)
                .httpOnly(true)
                .secure(properties.cookieSecure())
                .sameSite(properties.cookieSameSite())
                .path(COOKIE_PATH);
        if (rememberMe) {
            builder.maxAge(ttl);
        }
        response.addHeader(HttpHeaders.SET_COOKIE, builder.build().toString());
    }

    public void clear(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie
                .from(properties.cookieName(), "")
                .httpOnly(true)
                .secure(properties.cookieSecure())
                .sameSite(properties.cookieSameSite())
                .path(COOKIE_PATH)
                .maxAge(Duration.ZERO)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
