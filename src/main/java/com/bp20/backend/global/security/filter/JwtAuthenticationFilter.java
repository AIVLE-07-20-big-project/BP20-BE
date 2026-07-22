package com.bp20.backend.global.security.filter;

import com.bp20.backend.api.user.domain.User;
import com.bp20.backend.api.user.repository.UserRepository;
import com.bp20.backend.global.exception.ApiException;
import com.bp20.backend.global.response.ErrorCode;
import com.bp20.backend.global.security.authorization.RoleAuthorityMapper;
import com.bp20.backend.global.security.handler.JsonAuthenticationEntryPoint;
import com.bp20.backend.global.security.jwt.BearerTokenResolver;
import com.bp20.backend.global.security.jwt.JwtTokenProvider;
import com.bp20.backend.global.security.principal.SecurityPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";

    private final BearerTokenResolver bearerTokenResolver;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final JsonAuthenticationEntryPoint authenticationEntryPoint;
    private final RoleAuthorityMapper roleAuthorityMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authorizationHeader = request.getHeader(AUTHORIZATION_HEADER);

        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            authenticate(request, authorizationHeader);
            filterChain.doFilter(request, response);
        } catch (ApiException e) {
            SecurityContextHolder.clearContext();
            request.setAttribute(ApiException.class.getName(), e);
            authenticationEntryPoint.commence(request, response, new BadCredentialsException(e.getMessage(), e));
        }
    }

    private void authenticate(HttpServletRequest request, String authorizationHeader) {
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            return;
        }

        String token = bearerTokenResolver.resolve(authorizationHeader);
        Long userId = jwtTokenProvider.extractUserId(token);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED_INVALID_TOKEN));

        if (!user.isActive()) {
            throw new ApiException(ErrorCode.FORBIDDEN_ACCESS_DENIED);
        }

        SecurityPrincipal principal = SecurityPrincipal.from(user);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                roleAuthorityMapper.getAuthorities(principal.role())
        );
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
