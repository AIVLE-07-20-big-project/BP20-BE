package com.bp20.backend.global.response;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public enum SuccessCode {

    SUCCESS_AUTH_LOGIN(HttpStatus.OK, "로그인이 완료되었습니다."),
    SUCCESS_AUTH_ME(HttpStatus.OK, "현재 사용자 정보를 조회했습니다."),

    SUCCESS_ADMIN_CREATE(HttpStatus.CREATED, "관리자를 생성했습니다."),
    SUCCESS_ADMIN_GET(HttpStatus.OK, "관리자 정보를 조회했습니다."),
    SUCCESS_ADMIN_INVITATION_CREATE(HttpStatus.CREATED, "관리자 초대를 생성했습니다."),
    SUCCESS_ADMIN_INVITATION_ACCEPT(HttpStatus.CREATED, "관리자 초대를 수락했습니다."),
    SUCCESS_ADMIN_STATUS_UPDATE(HttpStatus.OK, "관리자 상태를 변경했습니다."),
    SUCCESS_IAM_LOG_GET(HttpStatus.OK, "IAM 로그를 조회했습니다."),

    SUCCESS_STORE_OWNER_GET(HttpStatus.OK, "점주 정보를 조회했습니다."),
    SUCCESS_STORE_OWNER_INVITATION_CREATE(HttpStatus.CREATED, "점주 초대를 생성했습니다."),
    SUCCESS_STORE_OWNER_INVITATION_ACCEPT(HttpStatus.CREATED, "점주 초대를 수락했습니다."),

    SUCCESS_STORE_CREATE(HttpStatus.CREATED, "매장을 생성했습니다."),
    SUCCESS_STORE_GET(HttpStatus.OK, "매장 정보를 조회했습니다."),
    SUCCESS_STORE_UPDATE(HttpStatus.OK, "매장 정보를 수정했습니다.");

    private final HttpStatus httpStatus;
    private final String message;

    public int getStatusCode() {
        return this.httpStatus.value();
    }
}
