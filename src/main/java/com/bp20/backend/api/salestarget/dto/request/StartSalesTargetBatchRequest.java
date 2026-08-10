package com.bp20.backend.api.salestarget.dto.request;

/**
 * topN이 없으면(요청 바디 자체를 생략해도) SalesTargetAiClient가 기본값 20을 채운다
 * (AI 서버 GenerateRequest의 기본값과 동일하게 맞춤).
 */
public record StartSalesTargetBatchRequest(Integer topN) {
}
