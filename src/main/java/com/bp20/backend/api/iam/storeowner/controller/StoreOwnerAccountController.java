package com.bp20.backend.api.iam.storeowner.controller;

import com.bp20.backend.api.iam.storeowner.dto.request.ChangeStoreOwnerStatusRequest;
import com.bp20.backend.api.iam.storeowner.dto.response.StoreOwnerAccountResponse;
import com.bp20.backend.api.iam.storeowner.dto.response.StoreOwnerPersonalDataResponse;
import com.bp20.backend.api.iam.dto.request.ReauthenticationRequest;
import com.bp20.backend.api.iam.storeowner.service.StoreOwnerAccountService;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/accounts/store-owners")
@Tag(name = "IAM - 점주 계정", description = "최고 관리자와 관리자가 점주 계정을 관리하는 API")
@SecurityRequirement(name = "bearerAuth")
public class StoreOwnerAccountController {

    private final StoreOwnerAccountService storeOwnerAccountService;

    @GetMapping
    @Operation(summary = "점주 계정 목록 조회")
    public ResponseEntity<ApiResponse<List<StoreOwnerAccountResponse>>> getStoreOwners() {
        return ApiResponse.success(
                SuccessCode.SUCCESS_STORE_OWNER_GET,
                storeOwnerAccountService.getStoreOwners()
        );
    }

    @PatchMapping("/{storeOwnerId}/deactivate")
    @Operation(summary = "점주 계정 비활성화")
    public ResponseEntity<ApiResponse<StoreOwnerAccountResponse>> deactivate(
            @AuthenticationPrincipal SecurityPrincipal currentUser,
            @PathVariable Long storeOwnerId,
            @Valid @RequestBody ChangeStoreOwnerStatusRequest request,
            HttpServletRequest servletRequest
    ) {
        return ApiResponse.success(
                SuccessCode.SUCCESS_STORE_OWNER_STATUS_UPDATE,
                storeOwnerAccountService.deactivate(
                        currentUser.id(),
                        storeOwnerId,
                        request.currentPassword(),
                        servletRequest.getRemoteAddr()
                )
        );
    }

    @PatchMapping("/{storeOwnerId}/activate")
    @Operation(summary = "점주 계정 활성화")
    public ResponseEntity<ApiResponse<StoreOwnerAccountResponse>> activate(
            @AuthenticationPrincipal SecurityPrincipal currentUser,
            @PathVariable Long storeOwnerId,
            @Valid @RequestBody ChangeStoreOwnerStatusRequest request,
            HttpServletRequest servletRequest
    ) {
        return ApiResponse.success(
                SuccessCode.SUCCESS_STORE_OWNER_STATUS_UPDATE,
                storeOwnerAccountService.activate(
                        currentUser.id(),
                        storeOwnerId,
                        request.currentPassword(),
                        servletRequest.getRemoteAddr()
                )
        );
    }

    @PostMapping("/{storeOwnerId}/personal-data/reveal")
    @Operation(
            summary = "점주 개인정보 원문 일시 조회",
            description = "현재 비밀번호로 재인증한 뒤 점주 개인정보 원문을 60초간 표시할 수 있도록 반환하며 IAM 로그를 기록합니다."
    )
    public ResponseEntity<ApiResponse<StoreOwnerPersonalDataResponse>> revealPersonalData(
            @AuthenticationPrincipal SecurityPrincipal currentUser,
            @PathVariable Long storeOwnerId,
            @Valid @RequestBody ReauthenticationRequest request,
            HttpServletRequest servletRequest
    ) {
        return ApiResponse.successNoStore(
                SuccessCode.SUCCESS_PERSONAL_DATA_REVEAL,
                storeOwnerAccountService.revealPersonalData(
                        currentUser.id(), storeOwnerId, request.currentPassword(), servletRequest.getRemoteAddr()
                )
        );
    }
}
