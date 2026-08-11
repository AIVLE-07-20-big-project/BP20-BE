package com.bp20.backend.api.productimage.client;

import com.bp20.backend.global.config.ProductImageServiceProperties;
import com.bp20.backend.global.response.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class ProductImageServiceClientTest {

    private final ProductImageServiceClient client = new ProductImageServiceClient(
            RestClient.builder(),
            new ProductImageServiceProperties("http://localhost:8000"),
            JsonMapper.builder().build()
    );

    @Test
    void mapsOpenAiModelErrorToServiceUnavailable() {
        String body = """
                {"detail":"safe message","error":{"code":"OPENAI_MODEL_UNAVAILABLE","message":"safe message"}}
                """;

        assertThat(client.resolveErrorCode(503, body))
                .isEqualTo(ErrorCode.PRODUCT_IMAGE_MODEL_UNAVAILABLE);
    }

    @Test
    void mapsOpenAiRateLimitToTooManyRequests() {
        String body = """
                {"error":{"code":"OPENAI_RATE_LIMITED","message":"safe message"}}
                """;

        assertThat(client.resolveErrorCode(429, body))
                .isEqualTo(ErrorCode.PRODUCT_IMAGE_RATE_LIMITED);
    }

    @Test
    void fallsBackToStatusWhenBodyIsNotJson() {
        assertThat(client.resolveErrorCode(504, "gateway timeout"))
                .isEqualTo(ErrorCode.PRODUCT_IMAGE_GENERATION_TIMEOUT);
    }
}
