package com.bp20.backend.global.response;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public enum ErrorCode {

    BAD_REQUEST_INVALID_INPUT(HttpStatus.BAD_REQUEST, "요청 입력값이 올바르지 않습니다."),
    BAD_REQUEST_INVALID_EMAIL(HttpStatus.BAD_REQUEST, "이메일 형식이 올바르지 않습니다."),
    BAD_REQUEST_INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "비밀번호 형식이 올바르지 않습니다."),
    BAD_REQUEST_INVALID_ROLE(HttpStatus.BAD_REQUEST, "사용자 역할이 올바르지 않습니다."),
    BAD_REQUEST_INVALID_FILE_EXTENSION(HttpStatus.BAD_REQUEST, "CSV 파일만 업로드할 수 있습니다."),

    UNAUTHORIZED_ACCESS(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    UNAUTHORIZED_INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),
    UNAUTHORIZED_INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "비밀번호가 올바르지 않습니다."),
    UNAUTHORIZED_EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다."),
    UNAUTHORIZED_TOKEN_EMPTY(HttpStatus.UNAUTHORIZED, "토큰이 비어 있습니다."),
    UNAUTHORIZED_INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "토큰이 올바르지 않습니다."),

    FORBIDDEN_ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    FORBIDDEN_ADMIN_REQUIRED(HttpStatus.FORBIDDEN, "관리자 권한이 필요합니다."),
    FORBIDDEN_SUPER_ADMIN_REQUIRED(HttpStatus.FORBIDDEN, "최상위 관리자 권한이 필요합니다."),

    NOT_FOUND_USER(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    NOT_FOUND_STORE(HttpStatus.NOT_FOUND, "매장을 찾을 수 없습니다."),
    NOT_FOUND_AI_ANALYSIS(HttpStatus.NOT_FOUND, "매출 분석 결과를 찾을 수 없습니다."),
    NOT_FOUND_AI_AGENT_RUN(HttpStatus.NOT_FOUND, "AI 에이전트 실행을 찾을 수 없습니다."),
    NOT_FOUND_SIGNUP_INVITATION(HttpStatus.NOT_FOUND, "유효한 회원가입 초대를 찾을 수 없습니다."),
    NOT_FOUND_RECEIPT(HttpStatus.NOT_FOUND, "영수증을 찾을 수 없습니다."),

    CONFLICT_DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    CONFLICT_DUPLICATE_BUSINESS_NUMBER(HttpStatus.CONFLICT, "이미 등록된 사업자등록번호입니다."),
    CONFLICT_STORE_OWNER_ALREADY_ASSIGNED(HttpStatus.CONFLICT, "이미 매장이 연결된 점주입니다."),
    CONFLICT_EXPIRED_SIGNUP_INVITATION(HttpStatus.CONFLICT, "회원가입 초대가 만료되었거나 더 이상 유효하지 않습니다."),
    CONFLICT_DUPLICATE_RECEIPT(HttpStatus.CONFLICT, "동일한 거래로 보이는 영수증이 이미 등록되어 있습니다."),

    OCR_SERVICE_UNAVAILABLE(HttpStatus.BAD_GATEWAY, "OCR/분석 서비스와 통신할 수 없습니다."),

    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final String message;

    public int getStatusCode() {
        return this.httpStatus.value();
    }
}