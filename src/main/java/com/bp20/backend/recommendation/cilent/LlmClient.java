package com.bp20.backend.llm;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class LlmClient {

    private final RestClient restClient;

    public LlmClient(
            @Qualifier("restClientBuilder")
            RestClient.Builder builder,
            @Value("${llm.server-url}") String serverUrl
    ) {
        this.restClient = builder.clone()
                .baseUrl(serverUrl)
                .build();
    }

    public String generateReason(
            LlmReasonRequest request
    ) {
        /*
         * 별도의 LLM 서버 또는 LLM API 프록시 서버에 요청한다.
         *
         * LLM에는 계산 결과만 전달한다.
         * LLM이 발주 수량을 새로 계산하거나 수정하지 못하도록
         * 프롬프트에서 명확하게 제한해야 한다.
         */
        LlmReasonResponse response = restClient.post()
                .uri("/api/v1/reasons")
                .body(request)
                .retrieve()
                .body(LlmReasonResponse.class);

        if (response == null || response.reason() == null) {
            /*
             * LLM 호출이 실패하면 규칙 기반 기본 설명을 반환하도록
             * fallback을 구현하는 것이 좋다.
             */
            return createFallbackReason(request);
        }

        return response.reason();
    }

    private String createFallbackReason(
            LlmReasonRequest request
    ) {
        return String.format(
                "%s의 향후 예상 사용량은 %d개입니다. "
                        + "현재 재고와 입고 예정 수량, 안전재고를 "
                        + "고려하여 %d개 발주를 추천합니다.",
                request.ingredientName(),
                request.expectedUsage(),
                request.recommendedOrderQuantity()
        );
    }
}
