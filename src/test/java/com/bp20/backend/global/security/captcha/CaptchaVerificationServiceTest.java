package com.bp20.backend.global.security.captcha;

import com.bp20.backend.global.exception.ApiException;
import com.bp20.backend.global.response.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class CaptchaVerificationServiceTest {

    private static final String VERIFY_URL = "https://www.google.com/recaptcha/api/siteverify";

    private RestClient.Builder builder;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
    }

    @Test
    void acceptsValidV3Response() {
        CaptchaVerificationService service = createService(0.5, "login", List.of("localhost"));
        respondWith("""
                {
                  "success": true,
                  "score": 0.9,
                  "action": "login",
                  "hostname": "localhost"
                }
                """);

        assertThatCode(() -> service.verify("valid-token", "127.0.0.1"))
                .doesNotThrowAnyException();
        server.verify();
    }

    @Test
    void rejectsResponseBelowMinimumScore() {
        CaptchaVerificationService service = createService(0.5, "login", List.of("localhost"));
        respondWith("""
                {
                  "success": true,
                  "score": 0.3,
                  "action": "login",
                  "hostname": "localhost"
                }
                """);

        assertInvalidCaptcha(() -> service.verify("low-score-token", "127.0.0.1"));
    }

    @Test
    void rejectsResponseWithoutScoreAsInvalidCaptcha() {
        CaptchaVerificationService service = createService(0.5, "login", List.of("localhost"));
        respondWith("""
                {
                  "success": false,
                  "score": null,
                  "hostname": "localhost",
                  "error-codes": ["invalid-input-response"]
                }
                """);

        assertInvalidCaptcha(() -> service.verify("invalid-token", "127.0.0.1"));
    }

    @Test
    void rejectsUnexpectedAction() {
        CaptchaVerificationService service = createService(0.5, "login", List.of("localhost"));
        respondWith("""
                {
                  "success": true,
                  "score": 0.9,
                  "action": "password_reset",
                  "hostname": "localhost"
                }
                """);

        assertInvalidCaptcha(() -> service.verify("wrong-action-token", "127.0.0.1"));
    }

    @Test
    void rejectsUnregisteredHostnameWhenAllowListIsConfigured() {
        CaptchaVerificationService service = createService(0.5, "login", List.of("localhost"));
        respondWith("""
                {
                  "success": true,
                  "score": 0.9,
                  "action": "login",
                  "hostname": "attacker.example"
                }
                """);

        assertInvalidCaptcha(() -> service.verify("wrong-host-token", "127.0.0.1"));
    }

    private CaptchaVerificationService createService(
            double minimumScore,
            String expectedAction,
            List<String> allowedHostnames
    ) {
        CaptchaProperties properties = new CaptchaProperties(
                true,
                "test-secret",
                VERIFY_URL,
                minimumScore,
                expectedAction,
                allowedHostnames
        );
        return new CaptchaVerificationService(properties, builder);
    }

    private void respondWith(String responseBody) {
        server.expect(requestTo(VERIFY_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));
    }

    private void assertInvalidCaptcha(ThrowingRunnable verification) {
        assertThatThrownBy(verification::run)
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.BAD_REQUEST_INVALID_CAPTCHA)
                );
        server.verify();
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run();
    }
}
