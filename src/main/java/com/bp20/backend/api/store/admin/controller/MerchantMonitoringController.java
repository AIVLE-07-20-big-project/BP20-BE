package com.bp20.backend.api.store.admin.controller;

import com.bp20.backend.api.store.admin.dto.MerchantMonitoringResponse;
import com.bp20.backend.api.store.admin.service.MerchantMonitoringService;
import com.bp20.backend.global.response.ApiResponse;
import com.bp20.backend.global.response.SuccessCode;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/merchants")
@Tag(name = "Admin - 매장 모니터링")
@SecurityRequirement(name = "bearerAuth")
public class MerchantMonitoringController {
    private final MerchantMonitoringService merchantMonitoringService;

    @GetMapping("/monitoring")
    @Operation(summary = "매장 모니터링 조회")
    public ResponseEntity<ApiResponse<MerchantMonitoringResponse>> getMerchants() {
        return ApiResponse.success(SuccessCode.SUCCESS_STORE_OWNER_GET, merchantMonitoringService.getMerchants());
    }
}
