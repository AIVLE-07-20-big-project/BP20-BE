package com.bp20.backend.api.ai.controller;

import com.bp20.backend.api.ai.dto.request.AgentRunResumeRequest;
import com.bp20.backend.api.ai.service.AiService;
import com.bp20.backend.global.exception.ApiException;
import com.bp20.backend.global.response.ApiResponse;
import com.bp20.backend.global.response.ErrorCode;
import com.bp20.backend.global.response.SuccessCode;
import com.bp20.backend.global.security.principal.SecurityPrincipal;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ai")
@SecurityRequirement(name = "bearerAuth")
public class AiController {

    private final AiService aiService;

    @PostMapping(value = "/analyses", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createAnalysis(
            @AuthenticationPrincipal SecurityPrincipal currentUser,
            @RequestParam MultipartFile file,
            @RequestParam("trdar_cd") @NotBlank String trdarCd,
            @RequestParam("svc_induty_cd") @NotBlank String svcIndutyCd,
            @RequestParam(value = "yyqu_cd", required = false) Integer yyquCd,
            @RequestParam(value = "store_id", required = false) String storeId
    ) {
        validateCsv(file);
        return ApiResponse.success(
                SuccessCode.SUCCESS_AI_ANALYSIS_CREATE,
                aiService.createAnalysis(currentUser.id(), storeId, file, trdarCd, svcIndutyCd, yyquCd)
        );
    }

    @GetMapping("/analyses/{analysisId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAnalysis(
            @AuthenticationPrincipal SecurityPrincipal currentUser,
            @PathVariable String analysisId
    ) {
        return ApiResponse.success(
                SuccessCode.SUCCESS_AI_ANALYSIS_GET,
                aiService.getAnalysis(currentUser.id(), analysisId)
        );
    }

    @PostMapping("/analyses/{analysisId}/recommendations")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createRecommendation(
            @AuthenticationPrincipal SecurityPrincipal currentUser,
            @PathVariable String analysisId
    ) {
        return ApiResponse.success(
                SuccessCode.SUCCESS_AI_RECOMMENDATION_CREATE,
                aiService.createRecommendation(currentUser.id(), analysisId)
        );
    }

    @GetMapping("/agent-runs/{threadId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAgentRun(
            @AuthenticationPrincipal SecurityPrincipal currentUser,
            @PathVariable String threadId
    ) {
        return ApiResponse.success(
                SuccessCode.SUCCESS_AI_AGENT_RUN_GET,
                aiService.getAgentRun(currentUser.id(), threadId)
        );
    }

    @PostMapping("/agent-runs/{threadId}/resume")
    public ResponseEntity<ApiResponse<Map<String, Object>>> resumeAgentRun(
            @AuthenticationPrincipal SecurityPrincipal currentUser,
            @PathVariable String threadId,
            @Valid @RequestBody AgentRunResumeRequest request
    ) {
        return ApiResponse.success(
                SuccessCode.SUCCESS_AI_AGENT_RUN_RESUME,
                aiService.resumeAgentRun(currentUser.id(), threadId, request)
        );
    }

    private void validateCsv(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".csv")) {
            throw new ApiException(ErrorCode.BAD_REQUEST_INVALID_FILE_EXTENSION);
        }
    }
}
