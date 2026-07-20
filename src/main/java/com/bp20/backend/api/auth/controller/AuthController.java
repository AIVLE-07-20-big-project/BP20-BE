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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "인증", description = "관리자와 점주의 로그인, 초대 기반 회원가입 및 현재 사용자 조회 API")
public class AuthController {

    private final LoginService loginService;
    private final SignupService signupService;

    @PostMapping("/signup")
    @Operation(
            summary = "초대 기반 회원가입",
            description = "관리자 또는 점주로 받은 이메일과 일회용 임시 비밀번호를 확인하고 회원가입을 완료합니다. 가입 역할은 초대 정보에서 자동으로 결정됩니다."
    )
    public ResponseEntity<ApiResponse<SignupResponse>> signup(
            @Valid @RequestBody SignupRequest request,
            HttpServletRequest servletRequest
    ) {
        return ApiResponse.success(
                SuccessCode.SUCCESS_AUTH_SIGNUP,
                signupService.signup(request, servletRequest.getRemoteAddr())
        );
    }

    @PostMapping("/login")
    @Operation(summary = "로그인", description = "이메일과 비밀번호를 검증하고 액세스 토큰을 발급합니다.")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request
    ) {
        return ApiResponse.success(SuccessCode.SUCCESS_AUTH_LOGIN, loginService.login(request));
    }

    @GetMapping("/me")
    @Operation(summary = "현재 사용자 조회", description = "Bearer 액세스 토큰으로 인증된 현재 사용자 정보를 조회합니다.")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<MeResponse>> getMe(
            @AuthenticationPrincipal SecurityPrincipal currentUser
    ) {
        return ApiResponse.success(SuccessCode.SUCCESS_AUTH_ME, loginService.getMe(currentUser.id()));
    }

}
