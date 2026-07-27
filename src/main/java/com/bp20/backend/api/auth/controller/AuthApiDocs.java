package com.bp20.backend.api.auth.controller;

import com.bp20.backend.api.auth.dto.request.LoginRequest;
import com.bp20.backend.api.auth.dto.request.SignupRequest;
import com.bp20.backend.api.auth.dto.response.LoginResponse;
import com.bp20.backend.api.auth.dto.response.MeResponse;
import com.bp20.backend.api.auth.dto.response.SignupResponse;
import com.bp20.backend.global.response.ApiResponse;
import com.bp20.backend.global.security.principal.SecurityPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;

@Tag(
        name = "인증",
        description = "관리자와 점주의 로그인, 초대 기반 회원가입 및 현재 사용자 조회 API"
)
public interface AuthApiDocs {

    @Operation(
            summary = "초대 기반 회원가입",
            description = "관리자 또는 점주로 받은 이메일과 일회용 임시 비밀번호를 확인하고 회원가입을 완료합니다. 가입 역할은 초대 정보에서 자동으로 결정됩니다.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = SignupRequest.class),
                            examples = {
                                    @ExampleObject(
                                            name = "일반 관리자 회원가입",
                                            summary = "ADMIN 초대 수락 및 회원가입",
                                            value = """
                                                    {
                                                      "email": "admin@bp20.com",
                                                      "temporaryPassword": "A7!kP3@mN8#qR5$t",
                                                      "password": "bp20admin001",
                                                      "name": "BP20 관리자",
                                                      "phoneNumber": "010-1111-1111"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "점주 회원가입",
                                            summary = "STORE_OWNER 초대 수락 및 회원가입",
                                            value = """
                                                    {
                                                      "email": "store-owner@bp20.com",
                                                      "temporaryPassword": "B8@mQ4#nR9$pS6%u",
                                                      "password": "bp20storeowner",
                                                      "name": "김점주",
                                                      "phoneNumber": "010-2222-2222"
                                                    }
                                                    """
                                    )
                            }
                    )
            )
    )
    ResponseEntity<ApiResponse<SignupResponse>> signup(
            SignupRequest request,
            @Parameter(hidden = true) HttpServletRequest servletRequest
    );

    @Operation(
            summary = "로그인",
            description = "이메일과 비밀번호를 검증하고 액세스 토큰을 발급합니다.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = LoginRequest.class),
                            examples = {
                                    @ExampleObject(
                                            name = "최고 관리자 로그인",
                                            summary = "SUPER_ADMIN 계정",
                                            value = """
                                                    {
                                                      "email": "super-admin@bp20.com",
                                                      "password": "bp20superadmin"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "일반 관리자 로그인",
                                            summary = "ADMIN 계정",
                                            value = """
                                                    {
                                                      "email": "admin@bp20.com",
                                                      "password": "bp20admin001"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "점주 로그인",
                                            summary = "STORE_OWNER 계정",
                                            value = """
                                                    {
                                                      "email": "store-owner@bp20.com",
                                                      "password": "bp20storeowner"
                                                    }
                                                    """
                                    )
                            }
                    )
            )
    )
    ResponseEntity<ApiResponse<LoginResponse>> login(LoginRequest request);

    @Operation(
            summary = "현재 사용자 조회",
            description = "Bearer 액세스 토큰으로 인증된 현재 사용자 정보를 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    ResponseEntity<ApiResponse<MeResponse>> getMe(
            @Parameter(hidden = true) SecurityPrincipal currentUser
    );
}
