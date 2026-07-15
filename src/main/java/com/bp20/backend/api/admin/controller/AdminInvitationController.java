package com.bp20.backend.api.admin.controller;

import com.bp20.backend.api.admin.dto.request.CreateAdminInvitationRequest;
import com.bp20.backend.api.admin.dto.response.AdminInvitationResponse;
import com.bp20.backend.api.admin.service.AdminInvitationService;
import com.bp20.backend.global.response.ApiResponse;
import com.bp20.backend.global.response.SuccessCode;
import com.bp20.backend.global.security.principal.SecurityPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/iam/admin-invitations")
@Tag(name = "IAM - 관리자 초대", description = "최상위 관리자가 일반 관리자를 초대하는 API")
@SecurityRequirement(name = "bearerAuth")
public class AdminInvitationController {

    private final AdminInvitationService adminInvitationService;

    @PostMapping
    @Operation(
            summary = "관리자 초대",
            description = "최상위 관리자 권한과 현재 비밀번호를 재확인한 뒤 16자리 일회용 임시 비밀번호를 생성합니다. 임시 비밀번호는 응답에서 한 번만 확인할 수 있습니다."
    )
    public ResponseEntity<ApiResponse<AdminInvitationResponse>> createInvitation(
            @AuthenticationPrincipal SecurityPrincipal currentUser,
            @Valid @RequestBody CreateAdminInvitationRequest request,
            HttpServletRequest servletRequest
    ) {
        return ApiResponse.success(
                SuccessCode.SUCCESS_ADMIN_INVITATION_CREATE,
                adminInvitationService.createInvitation(
                        currentUser.id(), request, servletRequest.getRemoteAddr()
                )
        );
    }
}
