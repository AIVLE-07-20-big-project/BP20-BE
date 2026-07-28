package com.bp20.backend.api.store.controller;

import com.bp20.backend.api.store.dto.request.CreateStoreRequest;
import com.bp20.backend.api.store.dto.request.UpdateStoreRequest;
import com.bp20.backend.api.store.dto.response.StoreResponse;
import com.bp20.backend.api.store.service.StoreService;
import com.bp20.backend.global.response.ApiResponse;
import com.bp20.backend.global.response.SuccessCode;
import com.bp20.backend.global.security.principal.SecurityPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/store-owner/stores")
@Tag(name = "점주 - 매장 관리", description = "점주가 자신에게 귀속된 매장 한 곳을 등록하고 관리하는 API")
@SecurityRequirement(name = "bearerAuth")
public class StoreController {

    private final StoreService storeService;

    @PostMapping
    @Operation(summary = "내 매장 등록", description = "점주 계정에 매장을 한 곳 등록합니다. 사업자등록번호는 중복될 수 없습니다.")
    public ResponseEntity<ApiResponse<StoreResponse>> create(
            @AuthenticationPrincipal SecurityPrincipal currentUser,
            @Valid @RequestBody CreateStoreRequest request
    ) {
        return ApiResponse.success(SuccessCode.SUCCESS_STORE_CREATE, storeService.create(currentUser.id(), request));
    }

    @GetMapping("/me")
    @Operation(summary = "내 매장 조회", description = "현재 로그인한 점주에게 귀속된 매장을 조회합니다.")
    public ResponseEntity<ApiResponse<StoreResponse>> getMine(
            @AuthenticationPrincipal SecurityPrincipal currentUser
    ) {
        return ApiResponse.success(SuccessCode.SUCCESS_STORE_GET, storeService.getMine(currentUser.id()));
    }

    @PutMapping("/me")
    @Operation(summary = "내 매장 수정", description = "매장명, 업종, 주소, 전화번호를 수정합니다.")
    public ResponseEntity<ApiResponse<StoreResponse>> updateMine(
            @AuthenticationPrincipal SecurityPrincipal currentUser,
            @Valid @RequestBody UpdateStoreRequest request
    ) {
        return ApiResponse.success(SuccessCode.SUCCESS_STORE_UPDATE, storeService.updateMine(currentUser.id(), request));
    }
}
