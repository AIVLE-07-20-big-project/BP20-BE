package com.bp20.backend.global.security.captcha;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app.auth.captcha")
public record CaptchaProperties(
        boolean enabled,
        String secretKey,
        String verifyUrl,
        Double minimumScore,
        String expectedAction,
        List<String> allowedHostnames
) {
    public CaptchaProperties {
        verifyUrl = verifyUrl == null || verifyUrl.isBlank()
                ? "https://www.google.com/recaptcha/api/siteverify"
                : verifyUrl;
        minimumScore = minimumScore == null ? 0.5 : minimumScore;
        if (minimumScore < 0.0 || minimumScore > 1.0) {
            throw new IllegalArgumentException("CAPTCHA_MIN_SCORE must be between 0.0 and 1.0.");
        }
        expectedAction = expectedAction == null || expectedAction.isBlank()
                ? "login"
                : expectedAction.trim();
        allowedHostnames = allowedHostnames == null
                ? List.of()
                : allowedHostnames.stream()
                .filter(hostname -> hostname != null && !hostname.isBlank())
                .map(String::trim)
                .toList();
    }
}
