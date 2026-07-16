package com.bp20.backend.global.logging;

import com.bp20.backend.global.exception.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class ExceptionLoggingAspect {

    @Pointcut("@within(org.springframework.stereotype.Service)")
    private void serviceLayer() {}

    @AfterThrowing(
            pointcut = "serviceLayer()",
            throwing = "ex"
    )
    public void logException(JoinPoint joinPoint, Exception ex) {

        String className = joinPoint.getSignature().getDeclaringType().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        if (ex instanceof ApiException apiEx) {

            log.warn("[Exception] {}.{}() | {} - {}",
                    className,
                    methodName,
                    apiEx.getClass().getSimpleName(),
                    apiEx.getMessage()
            );
        } else {
            log.error("[Exception] {}.{}() | {} - {}",
                    className,
                    methodName,
                    ex.getClass().getSimpleName(),
                    ex.getMessage(),
                    ex
            );
        }
    }
}
