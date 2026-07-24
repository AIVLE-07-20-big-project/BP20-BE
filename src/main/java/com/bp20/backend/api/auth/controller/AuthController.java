package com.bp20.backend.api.auth.controller;

import com.bp20.backend.api.auth.dto.request.LoginRequest;
import com.bp20.backend.api.auth.dto.request.SignupRequest;
import com.bp20.backend.api.auth.dto.response.LoginResponse;
import com.bp20.backend.api.auth.dto.response.MeResponse;
import com.bp20.backend.api.auth.dto.response.SignupResponse;
import com.bp20.backend.api.auth.service.LoginService;
import com.bp20.backend.api.auth.service.SignupService;
import com.bp20.backend.global.security.principal.SecurityPrincipal;
import com.bp20.backend.global.response.ApiResponse;
import com.bp20.backend.global.response.SuccessCode;
import jakarta.servlet.http.HttpServletRequest;
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

    @Override
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignupResponse>> signup(
            @Valid @RequestBody SignupRequest request,
            HttpServletRequest servletRequest
    ) {
        return ApiResponse.success(
                SuccessCode.SUCCESS_AUTH_SIGNUP,
                signupService.signup(request, servletRequest.getRemoteAddr())
        );
    }

    @Override
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request
    ) {
        return ApiResponse.success(SuccessCode.SUCCESS_AUTH_LOGIN, loginService.login(request));
    }

    @Override
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MeResponse>> getMe(
            @AuthenticationPrincipal SecurityPrincipal currentUser
    ) {
        return ApiResponse.success(SuccessCode.SUCCESS_AUTH_ME, loginService.getMe(currentUser.id()));
    }

}
