package com.bp20.backend.api.salestarget.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SalesTargetItemRequest(
        @NotBlank String businessName,
        String industry,
        @NotBlank String address,
        @NotNull Double totalScore,
        Double growthScore,
        Double trafficScore,
        Double reviewScore,
        Double similarityScore,
        // AI 서버 graph.py의 generate_pitch 노드(2단계)가 채우는 세일즈 피칭 문구.
        // null이어도 되게 열어둔다 — 옛 배치 결과 재전송이나 LLM 실패 fallback 케이스에서도 전체
        // 요청이 거부되면 안 되기 때문(다른 필드처럼 @NotBlank를 걸지 않는다).
        String salesPitch
) {
}
