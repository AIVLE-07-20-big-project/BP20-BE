package com.bp20.backend.api.admin.controller;

import com.bp20.backend.api.admin.dto.request.UpdateAdminStatusRequest;
import com.bp20.backend.api.admin.dto.response.AdminAccountResponse;
import com.bp20.backend.api.admin.service.AdminAccountService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/iam/admins")
@Tag(name = "IAM - 관리자 관리", description = "최상위 관리자가 관리자 계정 상태를 관리하는 API")
@SecurityRequirement(name = "bearerAuth")
public class AdminAccountController {

    private final AdminAccountService adminAccountService;

    @GetMapping
    @Operation(summary = "관리자 목록 조회", description = "최상위 관리자와 일반 관리자를 최신순으로 조회합니다.")
    public ResponseEntity<ApiResponse<List<AdminAccountResponse>>> getAdmins() {
        return ApiResponse.success(SuccessCode.SUCCESS_ADMIN_GET, adminAccountService.getAdmins());
    }

    @PatchMapping("/{adminId}/deactivate")
    @Operation(summary = "관리자 비활성화", description = "관리자를 비활성화하며 기존 JWT는 다음 요청부터 거부됩니다.")
    public ResponseEntity<ApiResponse<AdminAccountResponse>> deactivateAdmin(
            @AuthenticationPrincipal SecurityPrincipal currentUser,
            @PathVariable Long adminId,
            @Valid @RequestBody UpdateAdminStatusRequest request,
            HttpServletRequest servletRequest
    ) {
        return ApiResponse.success(
                SuccessCode.SUCCESS_ADMIN_STATUS_UPDATE,
                adminAccountService.deactivateAdmin(
                        currentUser.id(), adminId, request.currentPassword(), servletRequest.getRemoteAddr()
                )
        );
    }

    @PatchMapping("/{adminId}/activate")
    @Operation(summary = "관리자 활성화", description = "비활성화된 일반 관리자를 다시 활성화합니다.")
    public ResponseEntity<ApiResponse<AdminAccountResponse>> activateAdmin(
            @AuthenticationPrincipal SecurityPrincipal currentUser,
            @PathVariable Long adminId,
            @Valid @RequestBody UpdateAdminStatusRequest request,
            HttpServletRequest servletRequest
    ) {
        return ApiResponse.success(
                SuccessCode.SUCCESS_ADMIN_STATUS_UPDATE,
                adminAccountService.activateAdmin(
                        currentUser.id(), adminId, request.currentPassword(), servletRequest.getRemoteAddr()
                )
        );
    }
}
