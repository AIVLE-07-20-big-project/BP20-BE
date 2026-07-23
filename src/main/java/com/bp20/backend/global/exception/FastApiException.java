package com.bp20.backend.global.exception;

import lombok.Getter;

@Getter
public class FastApiException extends RuntimeException {

    private final int statusCode;

    public FastApiException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }
}
