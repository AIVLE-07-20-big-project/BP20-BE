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

    SUCCESS_AI_REPORT_CREATE(HttpStatus.OK, "AI 매출 분석 보고서를 생성했습니다."),
    SUCCESS_AI_ANALYSIS_CREATE(HttpStatus.OK, "매출 파일 분석을 시작했습니다."),
    SUCCESS_AI_ANALYSIS_GET(HttpStatus.OK, "매출 분석 결과를 조회했습니다."),
    SUCCESS_AI_ANALYSIS_REPORT_CREATE(HttpStatus.OK, "매출 분석 리포트를 생성했습니다."),
    SUCCESS_AI_AGENT_RUN_CREATE(HttpStatus.OK, "AI 에이전트 실행을 생성했습니다."),
    SUCCESS_AI_AGENT_RUN_GET(HttpStatus.OK, "AI 에이전트 실행을 조회했습니다."),
    SUCCESS_AI_AGENT_RUN_RESUME(HttpStatus.OK, "AI 에이전트 실행을 재개했습니다."),
    SUCCESS_AI_CAMPAIGN_LOG_CREATE(HttpStatus.OK, "AI 캠페인 로그를 생성했습니다."),
    SUCCESS_AI_CAMPAIGN_LOG_QUALITY_GET(HttpStatus.OK, "AI 캠페인 로그 품질을 조회했습니다.");

    private final HttpStatus httpStatus;
    private final String message;

    public int getStatusCode() {
        return this.httpStatus.value();
    }
}
