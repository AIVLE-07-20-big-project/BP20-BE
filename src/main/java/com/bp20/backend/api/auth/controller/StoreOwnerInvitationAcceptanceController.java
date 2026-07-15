package com.bp20.backend.api.auth.controller;

import com.bp20.backend.api.auth.dto.request.AcceptStoreOwnerInvitationRequest;
import com.bp20.backend.api.auth.dto.response.LoginResponse;
import com.bp20.backend.api.auth.service.StoreOwnerInvitationAcceptanceService;
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
@RequestMapping("/api/auth/store-owner-invitations")
@Tag(name = "인증 - 점주 초대", description = "점주가 임시 비밀번호로 초대를 수락하고 새 비밀번호를 설정하는 API")
public class StoreOwnerInvitationAcceptanceController {

    private final StoreOwnerInvitationAcceptanceService invitationAcceptanceService;

    @PostMapping("/accept")
    @Operation(
            summary = "점주 초대 수락",
            description = "이메일과 일회용 임시 비밀번호를 확인한 뒤 점주 본인이 새 비밀번호와 프로필을 설정합니다."
    )
    public ResponseEntity<ApiResponse<LoginResponse>> acceptInvitation(
            @Valid @RequestBody AcceptStoreOwnerInvitationRequest request,
            HttpServletRequest servletRequest
    ) {
        return ApiResponse.success(
                SuccessCode.SUCCESS_STORE_OWNER_INVITATION_ACCEPT,
                invitationAcceptanceService.acceptInvitation(request, servletRequest.getRemoteAddr())
        );
    }
}
