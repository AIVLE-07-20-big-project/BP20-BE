package com.bp20.backend.global.exeption.advice;

import com.bp20.backend.global.exeption.BaseException;
import com.bp20.backend.global.response.ApiResponse;
import com.bp20.backend.global.response.ErrorStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionAdvice {

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ApiResponse<Void>> handleBaseException(BaseException e) {

        return ResponseEntity
                .status(e.getStatusCode())
                .body(ApiResponse.failOnly(e.getErrorStatus()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {

        String message = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(this::formatFieldError)
                .orElse(ErrorStatus.BAD_REQUEST_INVALID_INPUT.getMessage());

        return ResponseEntity
                .status(ErrorStatus.BAD_REQUEST_INVALID_INPUT.getStatusCode())
                .body(ApiResponse.fail(ErrorStatus.BAD_REQUEST_INVALID_INPUT.getStatusCode(), message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {

        log.error("[UnhandledException]", e);

        return ResponseEntity
                .status(500)
                .body(ApiResponse.fail(500, ErrorStatus.INTERNAL_SERVER_ERROR.getMessage()));
    }

    private String formatFieldError(FieldError error) {
        return error.getField() + ": " + error.getDefaultMessage();
    }
}
