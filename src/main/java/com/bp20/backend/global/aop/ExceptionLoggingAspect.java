package com.bp20.backend.global.aop;

import com.bp20.backend.global.exeption.BaseException;
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

    @Pointcut("execution(* com.bp20.backend.api..service..*(..))")
    private void applicationLayer() {}

    // 메서드를 실행하다가 에러(Exception)가 터졌을 때만 작동
    @AfterThrowing(
            pointcut = "applicationLayer()",
            throwing = "ex"
    )
    public void logException(JoinPoint joinPoint, Exception ex) {

        String className = joinPoint.getSignature().getDeclaringType().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();

        if (ex instanceof BaseException baseEx) {

            log.warn("[Exception] {}.{}() | args={} | {} - {}",
                    className,
                    methodName,
                    args,
                    baseEx.getClass().getSimpleName(),
                    baseEx.getMessage()
            );
        } else {
            log.error("[Exception] {}.{}() | args={} | {} - {}",
                    className,
                    methodName,
                    args,
                    ex.getClass().getSimpleName(),
                    ex.getMessage(),
                    ex
            );
        }
    }
}
