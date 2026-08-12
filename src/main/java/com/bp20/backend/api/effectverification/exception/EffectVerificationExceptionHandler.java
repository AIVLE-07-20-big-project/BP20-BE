package com.bp20.backend.api.effectverification.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class EffectVerificationExceptionHandler {

    @ExceptionHandler(EffectVerificationAiException.class)
    public ResponseEntity<Map<String, Object>> handleAiException(
            EffectVerificationAiException exception
    ) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.BAD_GATEWAY.value());
        body.put("error", "효과 검증 AI 연동 실패");
        body.put("message", exception.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(body);
    }
}
