package com.bp20.backend.global.security.filter;

import com.bp20.backend.global.response.ApiResponse;
import com.bp20.backend.global.response.ErrorCode;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * AI 서버(FastAPI)가 호출하는 /api/internal/** 경로를 보호하는 필터.
 * 점주/관리자 JWT가 아니라 서비스 간 고정 키(X-Internal-Api-Key 헤더)로 인증한다.
 *
 * SecurityConfig에서:
 *  - 이 필터를 JwtAuthenticationFilter보다 먼저 태우고
 *  - "/api/internal/**" 경로는 permitAll로 열어둔다
 * (JWT가 없는 서비스 간 호출이라 permitAll이 맞다 — 실질적인 인증은 이 필터가 담당한다.)
 */
@Component
@RequiredArgsConstructor
public class InternalApiKeyFilter extends OncePerRequestFilter {

    private static final String INTERNAL_PATH_PREFIX = "/api/internal/";
    private static final String API_KEY_HEADER = "X-Internal-Api-Key";

    private final ObjectMapper objectMapper;

    @Value("${internal-api.key:}")
    private String internalApiKey;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (!request.getRequestURI().startsWith(INTERNAL_PATH_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String providedKey = request.getHeader(API_KEY_HEADER);
        if (internalApiKey == null || internalApiKey.isBlank()
                || providedKey == null || !providedKey.equals(internalApiKey)) {
            writeUnauthorized(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void writeUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(ErrorCode.UNAUTHORIZED_INVALID_INTERNAL_API_KEY.getStatusCode());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(
                objectMapper.writeValueAsString(
                        ApiResponse.failOnly(ErrorCode.UNAUTHORIZED_INVALID_INTERNAL_API_KEY)
                )
        );
    }
}
