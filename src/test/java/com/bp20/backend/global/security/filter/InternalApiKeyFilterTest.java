package com.bp20.backend.global.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InternalApiKeyFilterTest {

    private static final String VALID_KEY = "test-internal-key";

    private InternalApiKeyFilter filter;

    @BeforeEach
    void setUp() throws Exception {
        filter = new InternalApiKeyFilter(new ObjectMapper());
        setInternalApiKey(filter, VALID_KEY);
    }

    @Test
    void nonInternalPathPassesThroughRegardlessOfKey() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getRequestURI()).thenReturn("/api/admin/sales-targets");

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(response, never()).setStatus(anyInt());
    }

    @Test
    void internalPathWithCorrectKeyPassesThrough() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getRequestURI()).thenReturn("/api/internal/stores/registry");
        when(request.getHeader("X-Internal-Api-Key")).thenReturn(VALID_KEY);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(response, never()).setStatus(anyInt());
    }

    @Test
    void internalPathWithWrongKeyIsRejectedWithJsonBody() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        StringWriter body = new StringWriter();
        when(request.getRequestURI()).thenReturn("/api/internal/stores/registry");
        when(request.getHeader("X-Internal-Api-Key")).thenReturn("wrong-key");
        when(response.getWriter()).thenReturn(new PrintWriter(body));

        filter.doFilterInternal(request, response, chain);

        verify(chain, never()).doFilter(request, response);
        verify(response).setStatus(401);
        assertThat(body.toString()).contains("\"success\":false");
    }

    @Test
    void internalPathWithMissingKeyIsRejected() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getRequestURI()).thenReturn("/api/internal/stores/registry");
        when(request.getHeader("X-Internal-Api-Key")).thenReturn(null);
        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));

        filter.doFilterInternal(request, response, chain);

        verify(chain, never()).doFilter(request, response);
        verify(response).setStatus(401);
    }

    @Test
    void unconfiguredKeyAlwaysRejectsInternalPath() throws Exception {
        // internal-api.key가 비어있으면(운영에서 설정을 빼먹은 경우) 무조건 막아야 한다 — fail closed.
        setInternalApiKey(filter, "");
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getRequestURI()).thenReturn("/api/internal/stores/registry");
        when(request.getHeader("X-Internal-Api-Key")).thenReturn(VALID_KEY);
        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));

        filter.doFilterInternal(request, response, chain);

        verify(chain, never()).doFilter(request, response);
    }

    private void setInternalApiKey(InternalApiKeyFilter target, String value) throws Exception {
        Field field = InternalApiKeyFilter.class.getDeclaredField("internalApiKey");
        field.setAccessible(true);
        field.set(target, value);
    }
}
