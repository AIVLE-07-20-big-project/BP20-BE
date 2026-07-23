package com.bp20.backend.api.iam.invitation.controller;

import com.bp20.backend.api.iam.invitation.dto.request.InvitationRequest;
import com.bp20.backend.api.iam.invitation.dto.response.InvitationResponse;
import com.bp20.backend.api.iam.invitation.service.InvitationService;
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
@RequestMapping("/api/iam/invitation")
@Tag(name = "IAM - 사용자 초대", description = "권한을 가진 관리자가 관리자 또는 점주를 초대하는 API")
@SecurityRequirement(name = "bearerAuth")
public class InvitationController {

    private final InvitationService invitationService;

    @PostMapping("/admin")
    @Operation(
            summary = "관리자 초대",
            description = "최상위 관리자의 비밀번호를 재확인한 뒤 관리자 회원가입에 사용할 일회용 임시 비밀번호를 발급합니다."
    )
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

    @PostMapping("/store-owner")
    @Operation(
            summary = "점주 초대",
            description = "관리자의 비밀번호를 재확인한 뒤 점주 회원가입에 사용할 일회용 임시 비밀번호를 발급합니다."
    )
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
}
