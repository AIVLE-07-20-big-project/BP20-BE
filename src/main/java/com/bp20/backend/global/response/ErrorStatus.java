package com.bp20.backend.global.response;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public enum ErrorStatus {

    BAD_REQUEST_INVALID_INPUT(HttpStatus.BAD_REQUEST, "Invalid request input."),
    BAD_REQUEST_INVALID_EMAIL(HttpStatus.BAD_REQUEST, "Invalid email format."),
    BAD_REQUEST_INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "Invalid password format."),
    BAD_REQUEST_INVALID_ROLE(HttpStatus.BAD_REQUEST, "Invalid role."),

    UNAUTHORIZED_ACCESS(HttpStatus.UNAUTHORIZED, "Authentication is required."),
    UNAUTHORIZED_INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "Invalid password."),
    UNAUTHORIZED_EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "Expired token."),
    UNAUTHORIZED_TOKEN_EMPTY(HttpStatus.UNAUTHORIZED, "Token is empty."),
    UNAUTHORIZED_INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "Invalid token."),

    FORBIDDEN_ACCESS_DENIED(HttpStatus.FORBIDDEN, "Access denied."),

    NOT_FOUND_USER(HttpStatus.NOT_FOUND, "User not found."),
    NOT_FOUND_STORE(HttpStatus.NOT_FOUND, "Store not found."),
    NOT_FOUND_STORE_MANAGER(HttpStatus.NOT_FOUND, "Store manager not found."),

    CONFLICT_DUPLICATE_EMAIL(HttpStatus.CONFLICT, "Email is already in use."),
    CONFLICT_DUPLICATE_BUSINESS_NUMBER(HttpStatus.CONFLICT, "Business number is already in use."),
    CONFLICT_DUPLICATE_STORE_MANAGER(HttpStatus.CONFLICT, "User is already assigned to this store."),

    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error.");

    private final HttpStatus httpStatus;
    private final String message;

    public int getStatusCode() {
        return this.httpStatus.value();
    }
}
