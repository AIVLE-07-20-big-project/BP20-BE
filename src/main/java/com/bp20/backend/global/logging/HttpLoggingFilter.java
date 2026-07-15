package com.bp20.backend.global.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 모든 HTTP 요청에 대해 로깅을 하는 필터 (4주차 필수 요구사항 1번)<br>
 *
 */
@Slf4j
@Component
@Order(1)
public class HttpLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String requestId = UUID.randomUUID().toString().substring(0, 8);
        MDC.put("requestId", requestId);

        long startTime = System.currentTimeMillis();

        try {
            filterChain.doFilter(request, response);
        } finally {
            long elapsed = System.currentTimeMillis() - startTime;

            String uri = request.getRequestURI();
            String clientIp = resolveClientIp(request);

            log.info("[{}] {} {} status={} elapsed={}ms ip={}",
                    requestId,
                    request.getMethod(),
                    uri,
                    response.getStatus(),
                    elapsed,
                    clientIp
            );

            MDC.clear();
        }
    }

    private String resolveClientIp(HttpServletRequest request) {

        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }
}
