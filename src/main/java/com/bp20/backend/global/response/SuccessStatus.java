package com.bp20.backend.global.response;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public enum SuccessStatus {

    SUCCESS_MERCHANT_CREATE(HttpStatus.CREATED, "Merchant created."),
    SUCCESS_MERCHANT_GET(HttpStatus.OK, "Merchant retrieved."),

    SUCCESS_STORE_CREATE(HttpStatus.CREATED, "Store created."),
    SUCCESS_STORE_GET(HttpStatus.OK, "Store retrieved."),
    SUCCESS_STORE_UPDATE(HttpStatus.OK, "Store updated."),
    SUCCESS_STORE_MANAGER_ASSIGN(HttpStatus.CREATED, "Store manager assigned."),
    SUCCESS_STORE_MANAGER_REMOVE(HttpStatus.OK, "Store manager removed.");

    private final HttpStatus httpStatus;
    private final String message;

    public int getStatusCode() {
        return this.httpStatus.value();
    }
}
