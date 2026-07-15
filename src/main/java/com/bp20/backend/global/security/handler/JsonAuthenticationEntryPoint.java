package com.bp20.backend.global.security.handler;

import com.bp20.backend.global.exception.ApiException;
import com.bp20.backend.global.response.ApiResponse;
import com.bp20.backend.global.response.ErrorCode;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        ErrorCode errorCode = resolveErrorCode(request);

        response.setStatus(errorCode.getStatusCode());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.failOnly(errorCode)));
    }

    private ErrorCode resolveErrorCode(HttpServletRequest request) {
        Object exception = request.getAttribute(ApiException.class.getName());
        if (exception instanceof ApiException apiException) {
            return apiException.getErrorCode();
        }
        return ErrorCode.UNAUTHORIZED_ACCESS;
    }
}
