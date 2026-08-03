package com.bp20.backend.api.auth.controller;

import com.bp20.backend.api.auth.dto.request.LoginRequest;
import com.bp20.backend.api.auth.dto.request.SignupRequest;
import com.bp20.backend.api.auth.dto.response.LoginResponse;
import com.bp20.backend.api.auth.dto.response.MeResponse;
import com.bp20.backend.api.auth.dto.response.SignupResponse;
import com.bp20.backend.api.auth.dto.response.TokenRefreshResponse;
import com.bp20.backend.api.auth.service.LoginService;
import com.bp20.backend.api.auth.service.SignupService;
import com.bp20.backend.api.auth.session.AuthenticatedSession;
import com.bp20.backend.api.auth.session.RefreshTokenCookieManager;
import com.bp20.backend.api.auth.session.RefreshTokenService;
import com.bp20.backend.global.exception.ApiException;
import com.bp20.backend.global.response.ApiResponse;
import com.bp20.backend.global.response.SuccessCode;
import com.bp20.backend.global.security.principal.SecurityPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController implements AuthApiDocs {

    private final LoginService loginService;
    private final SignupService signupService;
    private final RefreshTokenService refreshTokenService;
    private final RefreshTokenCookieManager refreshTokenCookieManager;

    @Override
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignupResponse>> signup(
            @Valid @RequestBody SignupRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse
    ) {
        AuthenticatedSession<SignupResponse> session =
                signupService.signup(request, servletRequest.getRemoteAddr());
        writeRefreshTokenCookie(servletResponse, session);
        return ApiResponse.success(SuccessCode.SUCCESS_AUTH_SIGNUP, session.response());
    }

    @Override
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse servletResponse
    ) {
        AuthenticatedSession<LoginResponse> session = loginService.login(request);
        writeRefreshTokenCookie(servletResponse, session);
        return ApiResponse.success(SuccessCode.SUCCESS_AUTH_LOGIN, session.response());
    }

    @Override
    @PostMapping("/token/refresh")
    public ResponseEntity<ApiResponse<TokenRefreshResponse>> refresh(
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse
    ) {
        try {
            RefreshTokenService.TokenPair tokenPair = refreshTokenService.rotate(
                    refreshTokenCookieManager.require(servletRequest)
            );
            refreshTokenCookieManager.write(
                    servletResponse,
                    tokenPair.refreshToken(),
                    tokenPair.refreshTokenTtl(),
                    tokenPair.rememberMe()
            );
            return ApiResponse.success(
                    SuccessCode.SUCCESS_AUTH_TOKEN_REFRESH,
                    TokenRefreshResponse.of(tokenPair.accessToken())
            );
        } catch (ApiException exception) {
            refreshTokenCookieManager.clear(servletResponse);
            throw exception;
        }
    }

    @Override
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse
    ) {
        refreshTokenCookieManager.read(servletRequest).ifPresent(refreshTokenService::logout);
        refreshTokenCookieManager.clear(servletResponse);
        return ApiResponse.successOnly(SuccessCode.SUCCESS_AUTH_LOGOUT);
    }

    @Override
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MeResponse>> getMe(
            @AuthenticationPrincipal SecurityPrincipal currentUser
    ) {
        return ApiResponse.success(SuccessCode.SUCCESS_AUTH_ME, loginService.getMe(currentUser.id()));
    }

    private void writeRefreshTokenCookie(
            HttpServletResponse servletResponse,
            AuthenticatedSession<?> session
    ) {
        refreshTokenCookieManager.write(
                servletResponse,
                session.refreshToken(),
                session.refreshTokenTtl(),
                session.rememberMe()
        );
    }
}
