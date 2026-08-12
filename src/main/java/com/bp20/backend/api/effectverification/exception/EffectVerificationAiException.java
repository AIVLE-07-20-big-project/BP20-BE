package com.bp20.backend.api.effectverification.exception;

public class EffectVerificationAiException extends RuntimeException {

    public EffectVerificationAiException(String message) {
        super(message);
    }

    public EffectVerificationAiException(String message, Throwable cause) {
        super(message, cause);
    }
}
