package com.bp20.backend.api.auth.controller;

import com.bp20.backend.api.auth.dto.request.AcceptAdminInvitationRequest;
import com.bp20.backend.api.auth.dto.response.LoginResponse;
import com.bp20.backend.api.auth.service.AdminInvitationAcceptanceService;
import com.bp20.backend.global.response.ApiResponse;
import com.bp20.backend.global.response.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth/admin-invitations")
@Tag(name = "인증 - 관리자 초대", description = "관리자가 임시 비밀번호로 초대를 수락하고 새 비밀번호를 설정하는 API")
public class AdminInvitationAcceptanceController {
    private final AdminInvitationAcceptanceService invitationAcceptanceService;

    @PostMapping("/accept")
    @Operation(
            summary = "관리자 초대 수락",
            description = "이메일과 일회용 임시 비밀번호를 확인한 뒤 관리자 본인이 새 비밀번호와 프로필을 설정합니다."
    )
    public ResponseEntity<ApiResponse<LoginResponse>> acceptInvitation(
            @Valid @RequestBody AcceptAdminInvitationRequest request,
            HttpServletRequest servletRequest
    ) {
        return ApiResponse.success(
                SuccessCode.SUCCESS_ADMIN_INVITATION_ACCEPT,
                invitationAcceptanceService.acceptInvitation(request, servletRequest.getRemoteAddr())
        );
    }
}
