package com.bp20.backend.api.admin.controller;

import com.bp20.backend.api.admin.dto.request.CreateStoreOwnerInvitationRequest;
import com.bp20.backend.api.admin.dto.response.StoreOwnerInvitationResponse;
import com.bp20.backend.api.admin.service.AdminStoreOwnerInvitationService;
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
@RequestMapping("/api/admin/store-owner-invitations")
@Tag(name = "관리자 - 점주 초대", description = "최상위 관리자 또는 일반 관리자가 점주를 초대하는 API")
@SecurityRequirement(name = "bearerAuth")
public class AdminStoreOwnerInvitationController {

    private final AdminStoreOwnerInvitationService invitationService;

    @PostMapping
    @Operation(
            summary = "점주 초대",
            description = "관리자 비밀번호를 재확인한 뒤 16자리 일회용 임시 비밀번호를 생성합니다. 임시 비밀번호는 응답에서 한 번만 확인할 수 있습니다."
    )
    public ResponseEntity<ApiResponse<StoreOwnerInvitationResponse>> createInvitation(
            @AuthenticationPrincipal SecurityPrincipal currentUser,
            @Valid @RequestBody CreateStoreOwnerInvitationRequest request,
            HttpServletRequest servletRequest
    ) {
        return ApiResponse.success(
                SuccessCode.SUCCESS_STORE_OWNER_INVITATION_CREATE,
                invitationService.createInvitation(currentUser.id(), request, servletRequest.getRemoteAddr())
        );
    }
}
