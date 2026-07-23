package com.bp20.backend.global.exception;

import com.bp20.backend.global.response.ApiResponse;
import com.bp20.backend.global.response.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MissingServletRequestParameterException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(FastApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleFastApiException(FastApiException e) {
        log.warn("[FastApiException] status={}, message={}", e.getStatusCode(), e.getMessage());
        return ResponseEntity
                .status(e.getStatusCode())
                .body(ApiResponse.fail(e.getStatusCode(), e.getMessage()));
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleApiException(
            ApiException e
    ) {
        return ResponseEntity
                .status(e.getStatusCode())
                .body(ApiResponse.failOnly(e.getErrorCode()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(
            MethodArgumentNotValidException e
    ) {
        String message = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(this::formatFieldError)
                .orElse(ErrorCode.BAD_REQUEST_INVALID_INPUT.getMessage());

        return ResponseEntity
                .status(ErrorCode.BAD_REQUEST_INVALID_INPUT.getStatusCode())
                .body(ApiResponse.fail(
                        ErrorCode.BAD_REQUEST_INVALID_INPUT.getStatusCode(),
                        message
                ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(
            IllegalArgumentException e
    ) {
        log.warn("[IllegalArgumentException] {}", e.getMessage(), e);

        return ResponseEntity
                .badRequest()
                .body(ApiResponse.fail(400, e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(
            Exception e
    ) {
        log.error("[UnhandledException]", e);

        return ResponseEntity
                .status(500)
                .body(ApiResponse.fail(
                        500,
                        ErrorCode.INTERNAL_SERVER_ERROR.getMessage()
                ));
    }

    private String formatFieldError(FieldError error) {
        return error.getField() + ": " + error.getDefaultMessage();
    }

}