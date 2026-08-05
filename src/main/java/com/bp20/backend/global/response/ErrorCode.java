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
    BAD_REQUEST_INVALID_PRODUCT_STATUS(HttpStatus.BAD_REQUEST, "상품 상태 변경 요청이 올바르지 않습니다."),
    BAD_REQUEST_INVALID_ONLINE_PRODUCT_STATUS(HttpStatus.BAD_REQUEST, "온라인 상품 등록 또는 해제 요청이 올바르지 않습니다."),
    BAD_REQUEST_INVALID_DISCOUNT(HttpStatus.BAD_REQUEST, "할인 설정이 올바르지 않습니다."),
    BAD_REQUEST_INVALID_COUPON(HttpStatus.BAD_REQUEST, "쿠폰 발급 또는 상태 변경 요청이 올바르지 않습니다."),
    BAD_REQUEST_PRIVACY_CONSENT_REQUIRED(HttpStatus.BAD_REQUEST, "필수 개인정보 수집 및 이용 동의가 필요합니다."),
    BAD_REQUEST_CAPTCHA_REQUIRED(HttpStatus.BAD_REQUEST, "자동입력 방지 확인이 필요합니다."),
    BAD_REQUEST_INVALID_CAPTCHA(HttpStatus.BAD_REQUEST, "자동입력 방지 확인에 실패했습니다. 다시 시도해 주세요."),

    UNAUTHORIZED_ACCESS(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    UNAUTHORIZED_INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),
    UNAUTHORIZED_INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "비밀번호가 올바르지 않습니다."),
    UNAUTHORIZED_EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다."),
    UNAUTHORIZED_TOKEN_EMPTY(HttpStatus.UNAUTHORIZED, "토큰이 비어 있습니다."),
    UNAUTHORIZED_INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "토큰이 올바르지 않습니다."),
    UNAUTHORIZED_INVALID_INTERNAL_API_KEY(HttpStatus.UNAUTHORIZED, "내부 서비스 인증키가 올바르지 않습니다."),
    UNAUTHORIZED_REFRESH_TOKEN_REQUIRED(HttpStatus.UNAUTHORIZED, "Refresh Token이 필요합니다."),
    UNAUTHORIZED_INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "Refresh Token이 올바르지 않습니다."),
    UNAUTHORIZED_EXPIRED_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "Refresh Token이 만료되었습니다."),
    UNAUTHORIZED_REFRESH_TOKEN_REUSED(HttpStatus.UNAUTHORIZED, "이미 사용된 Refresh Token이 감지되어 로그인 세션을 종료했습니다."),

    FORBIDDEN_ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    FORBIDDEN_ADMIN_REQUIRED(HttpStatus.FORBIDDEN, "관리자 권한이 필요합니다."),
    FORBIDDEN_SUPER_ADMIN_REQUIRED(HttpStatus.FORBIDDEN, "최상위 관리자 권한이 필요합니다."),
    FORBIDDEN_STORE_OWNER_REQUIRED(HttpStatus.FORBIDDEN, "점주 권한이 필요합니다."),
    FORBIDDEN_PASSWORD_EXPIRED(HttpStatus.FORBIDDEN, "비밀번호 사용 기간이 만료되었습니다. 관리자에게 재설정을 요청해 주세요."),

    LOCKED_LOGIN_ACCOUNT(HttpStatus.LOCKED, "로그인 실패 횟수를 초과해 계정이 일시적으로 잠겼습니다."),

    NOT_FOUND_USER(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    NOT_FOUND_INVITATION(HttpStatus.NOT_FOUND, "초대를 찾을 수 없습니다."),
    NOT_FOUND_STORE(HttpStatus.NOT_FOUND, "매장을 찾을 수 없습니다."),
    NOT_FOUND_AI_ANALYSIS(HttpStatus.NOT_FOUND, "매출 분석 결과를 찾을 수 없습니다."),
    NOT_FOUND_AI_AGENT_RUN(HttpStatus.NOT_FOUND, "AI 에이전트 실행을 찾을 수 없습니다."),
    NOT_FOUND_PRODUCT(HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다."),
    NOT_FOUND_DISCOUNT(HttpStatus.NOT_FOUND, "할인을 찾을 수 없습니다."),
    NOT_FOUND_CUSTOMER(HttpStatus.NOT_FOUND, "고객을 찾을 수 없습니다."),
    NOT_FOUND_COUPON(HttpStatus.NOT_FOUND, "쿠폰을 찾을 수 없습니다."),
    NOT_FOUND_SIGNUP_INVITATION(HttpStatus.NOT_FOUND, "유효한 회원가입 초대를 찾을 수 없습니다."),
    NOT_FOUND_RECEIPT(HttpStatus.NOT_FOUND, "영수증을 찾을 수 없습니다."),
    NOT_FOUND_SALES_TARGET(HttpStatus.NOT_FOUND, "존재하지 않는 영업 타겟입니다."),
    NOT_FOUND_SALES_TARGET_BATCH_RUN(HttpStatus.NOT_FOUND, "존재하지 않는 영업 타겟 배치 실행입니다."),
    NOT_FOUND_NOTICE(HttpStatus.NOT_FOUND, "공지를 찾을 수 없습니다."),

    CONFLICT_DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    CONFLICT_DUPLICATE_BUSINESS_NUMBER(HttpStatus.CONFLICT, "이미 등록된 사업자등록번호입니다."),
    CONFLICT_STORE_OWNER_ALREADY_ASSIGNED(HttpStatus.CONFLICT, "이미 매장에 연결된 점주입니다."),
    CONFLICT_STORE_ALREADY_EXISTS(HttpStatus.CONFLICT, "점주에게 이미 등록된 매장이 있습니다."),
    CONFLICT_DUPLICATE_CUSTOMER_EMAIL(HttpStatus.CONFLICT, "해당 매장에 이미 등록된 고객 이메일입니다."),
    CONFLICT_EXPIRED_SIGNUP_INVITATION(HttpStatus.CONFLICT, "회원가입 초대가 만료되었거나 더 이상 유효하지 않습니다."),
    CONFLICT_INVITATION_NOT_REVOCABLE(HttpStatus.CONFLICT, "대기 중인 초대만 취소할 수 있습니다."),
    CONFLICT_DUPLICATE_RECEIPT(HttpStatus.CONFLICT, "동일한 거래로 보이는 영수증이 이미 등록되어 있습니다."),
    CONFLICT_SALES_TARGET_BATCH_STILL_PENDING(HttpStatus.CONFLICT, "승인 대기 중인 배치는 삭제할 수 없습니다. 먼저 승인하거나 반려해 주세요."),

    OCR_SERVICE_UNAVAILABLE(HttpStatus.BAD_GATEWAY, "OCR/분석 서비스와 통신할 수 없습니다."),
    PRODUCT_IMAGE_SERVICE_UNAVAILABLE(HttpStatus.BAD_GATEWAY, "AI 상품 이미지 생성 서비스와 통신할 수 없습니다."),
    SERVICE_UNAVAILABLE_CAPTCHA(HttpStatus.SERVICE_UNAVAILABLE, "자동입력 방지 확인 서비스를 사용할 수 없습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final String message;

    public int getStatusCode() {
        return this.httpStatus.value();
    }
}
