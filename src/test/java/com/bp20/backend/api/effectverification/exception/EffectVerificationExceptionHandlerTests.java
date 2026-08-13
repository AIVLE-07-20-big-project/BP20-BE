package com.bp20.backend.api.effectverification.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class EffectVerificationExceptionHandlerTests {

    private final EffectVerificationExceptionHandler handler =
            new EffectVerificationExceptionHandler();

    @Test
    void returnsBadGatewayForAiIntegrationFailure() {
        var response = handler.handleAiException(
                new EffectVerificationAiException("AI 호출 실패")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(response.getBody()).containsEntry(
                "error",
                "효과 검증 AI 연동 실패"
        );
        assertThat(response.getBody()).containsEntry("message", "AI 호출 실패");
    }
}
