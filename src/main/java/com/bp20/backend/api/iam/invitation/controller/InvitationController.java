package com.bp20.backend.api.iam.invitation.controller;

import com.bp20.backend.api.iam.invitation.dto.request.InvitationRequest;
import com.bp20.backend.api.iam.invitation.dto.request.RevokeInvitationRequest;
import com.bp20.backend.api.iam.invitation.dto.response.InvitationResponse;
import com.bp20.backend.api.iam.invitation.dto.response.InvitationSummaryResponse;
import com.bp20.backend.api.iam.invitation.service.InvitationService;
import com.bp20.backend.global.response.ApiResponse;
import com.bp20.backend.global.response.SuccessCode;
import com.bp20.backend.global.security.principal.SecurityPrincipal;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/iam/invitation")
@Tag(name = "IAM - 사용자 초대", description = "권한을 가진 관리자가 관리자 또는 점주를 초대하는 API")
@SecurityRequirement(name = "bearerAuth")
public class InvitationController implements InvitationApiDocs {

    private final InvitationService invitationService;

@GetMapping
@io.swagger.v3.oas.annotations.Operation(summary = "초대 목록 조회", description = "SUPER_ADMIN은 관리자·점주 초대를, ADMIN은 점주 초대만 조회합니다.")
public ResponseEntity<ApiResponse<List<InvitationSummaryResponse>>> getInvitations(
            @AuthenticationPrincipal SecurityPrincipal currentUser
    ) {
        return ApiResponse.success(
                SuccessCode.SUCCESS_INVITATION_GET,
                invitationService.getInvitations(currentUser.role())
        );
    }

    @Override
    @PostMapping("/admin")
    public ResponseEntity<ApiResponse<InvitationResponse>> inviteAdmin(
            @AuthenticationPrincipal SecurityPrincipal currentUser,
            @Valid @RequestBody InvitationRequest request,
            HttpServletRequest servletRequest
    ) {
        return ApiResponse.success(
                SuccessCode.SUCCESS_ADMIN_INVITATION_CREATE,
                invitationService.inviteAdmin(currentUser.id(), request, servletRequest.getRemoteAddr())
        );
    }

    @Override
    @PostMapping("/store-owner")
    public ResponseEntity<ApiResponse<InvitationResponse>> inviteStoreOwner(
            @AuthenticationPrincipal SecurityPrincipal currentUser,
            @Valid @RequestBody InvitationRequest request,
            HttpServletRequest servletRequest
    ) {
        return ApiResponse.success(
                SuccessCode.SUCCESS_STORE_OWNER_INVITATION_CREATE,
                invitationService.inviteStoreOwner(currentUser.id(), request, servletRequest.getRemoteAddr())
        );
    }

@PatchMapping("/{invitationId}/revoke")
@io.swagger.v3.oas.annotations.Operation(summary = "초대 취소", description = "현재 비밀번호를 재확인한 뒤, 대기 중(PENDING) 초대만 취소(REVOKED)할 수 있습니다.")
public ResponseEntity<ApiResponse<InvitationSummaryResponse>> revokeInvitation(
            @AuthenticationPrincipal SecurityPrincipal currentUser,
            @PathVariable Long invitationId,
            @Valid @RequestBody RevokeInvitationRequest request,
            HttpServletRequest servletRequest
    ) {
        return ApiResponse.success(
                SuccessCode.SUCCESS_INVITATION_REVOKE,
                invitationService.revokeInvitation(
                        currentUser.id(),
                        invitationId,
                        request.currentPassword(),
                        servletRequest.getRemoteAddr()
                )
        );
    }
}
