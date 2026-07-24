package com.bp20.backend.global.response;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public enum SuccessCode {

    SUCCESS_AUTH_LOGIN(HttpStatus.OK, "로그인이 완료되었습니다."),
    SUCCESS_AUTH_SIGNUP(HttpStatus.CREATED, "회원가입이 완료되었습니다."),
    SUCCESS_AUTH_ME(HttpStatus.OK, "현재 사용자 정보를 조회했습니다."),

    SUCCESS_ADMIN_CREATE(HttpStatus.CREATED, "관리자를 생성했습니다."),
    SUCCESS_ADMIN_GET(HttpStatus.OK, "관리자 정보를 조회했습니다."),
    SUCCESS_ADMIN_INVITATION_CREATE(HttpStatus.CREATED, "관리자 초대를 생성했습니다."),
    SUCCESS_ADMIN_STATUS_UPDATE(HttpStatus.OK, "관리자 상태를 변경했습니다."),
    SUCCESS_IAM_LOG_GET(HttpStatus.OK, "IAM 로그를 조회했습니다."),

    SUCCESS_STORE_OWNER_GET(HttpStatus.OK, "점주 정보를 조회했습니다."),
    SUCCESS_STORE_OWNER_INVITATION_CREATE(HttpStatus.CREATED, "점주 초대를 생성했습니다."),

    SUCCESS_STORE_CREATE(HttpStatus.CREATED, "매장을 생성했습니다."),
    SUCCESS_STORE_GET(HttpStatus.OK, "매장 정보를 조회했습니다."),
    SUCCESS_STORE_UPDATE(HttpStatus.OK, "매장 정보를 수정했습니다."),

    SUCCESS_AI_ANALYSIS_CREATE(HttpStatus.OK, "매출 분석이 완료되었습니다."),
    SUCCESS_AI_ANALYSIS_GET(HttpStatus.OK, "매출 분석 결과를 조회했습니다."),
    SUCCESS_AI_RECOMMENDATION_CREATE(HttpStatus.OK, "고객 대응방안 추천과 검증을 시작했습니다."),
    SUCCESS_AI_RECOMMENDATION_GET(HttpStatus.OK, "AI 전략 추천 이력을 조회했습니다."),
    SUCCESS_AI_AGENT_RUN_GET(HttpStatus.OK, "AI 에이전트 실행을 조회했습니다."),
    SUCCESS_AI_AGENT_RUN_RESUME(HttpStatus.OK, "AI 에이전트 실행을 재개했습니다."),
    SUCCESS_RECEIPT_PARSE(HttpStatus.OK, "영수증 OCR 인식이 완료되었습니다."),
    SUCCESS_RECEIPT_CREATE(HttpStatus.CREATED, "영수증을 저장했습니다."),
    SUCCESS_RECEIPT_GET(HttpStatus.OK, "영수증 정보를 조회했습니다."),

    SUCCESS_ANALYTICS_EXPENSE_ANOMALIES(HttpStatus.OK, "이상 지출 탐지 결과를 조회했습니다."),
    SUCCESS_ANALYTICS_BUDGET_OVERAGE(HttpStatus.OK, "예산 초과 확인 결과를 조회했습니다."),
    SUCCESS_BUDGET_CREATE(HttpStatus.CREATED, "예산을 등록했습니다."),

    SUCCESS_ONLINE_SALES_STATUS_UPDATE(HttpStatus.OK, "온라인 판매 상태를 변경했습니다."),
    SUCCESS_PRODUCT_CREATE(HttpStatus.CREATED, "상품을 등록했습니다."),
    SUCCESS_PRODUCT_GET(HttpStatus.OK, "상품을 조회했습니다."),
    SUCCESS_PRODUCT_UPDATE(HttpStatus.OK, "상품을 수정했습니다."),
    SUCCESS_ONLINE_PRODUCT_REGISTER(HttpStatus.CREATED, "상품을 온라인 판매에 등록했습니다."),
    SUCCESS_ONLINE_PRODUCT_UNREGISTER(HttpStatus.OK, "상품의 온라인 판매 등록을 해제했습니다."),

    SUCCESS_DISCOUNT_CREATE(HttpStatus.CREATED, "할인을 등록했습니다."),
    SUCCESS_DISCOUNT_GET(HttpStatus.OK, "할인을 조회했습니다."),
    SUCCESS_DISCOUNT_UPDATE(HttpStatus.OK, "할인 상태를 변경했습니다."),

    SUCCESS_COUPON_ISSUE(HttpStatus.CREATED, "고객에게 쿠폰을 발급했습니다."),
    SUCCESS_COUPON_GET(HttpStatus.OK, "쿠폰을 조회했습니다."),
    SUCCESS_COUPON_UPDATE(HttpStatus.OK, "쿠폰 상태를 변경했습니다."),

    SUCCESS_CUSTOMER_CREATE(HttpStatus.CREATED, "고객을 등록했습니다."),
    SUCCESS_CUSTOMER_GET(HttpStatus.OK, "고객을 조회했습니다.");

    private final HttpStatus httpStatus;
    private final String message;

    public int getStatusCode() {
        return this.httpStatus.value();
    }
}
