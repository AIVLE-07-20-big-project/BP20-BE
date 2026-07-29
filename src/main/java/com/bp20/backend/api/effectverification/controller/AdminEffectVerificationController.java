package com.bp20.backend.api.effectverification.controller;

import com.bp20.backend.api.effectverification.dto.response.EffectVerificationRoiResponse;
import com.bp20.backend.api.effectverification.service.EffectVerificationRoiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/effect-verifications")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class AdminEffectVerificationController {

    private final EffectVerificationRoiService roiService;

    @GetMapping("/roi")
    @Operation(summary = "효과 검증 ROI 통계 조회")
    public ResponseEntity<EffectVerificationRoiResponse> getRoiSummary(
            @RequestParam(name = "store_id", required = false) Long storeId
    ) {
        return ResponseEntity.ok(roiService.getSummary(storeId));
    }
}
