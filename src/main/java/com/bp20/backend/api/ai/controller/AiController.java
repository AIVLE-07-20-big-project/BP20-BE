package com.bp20.backend.api.ai.controller;

import com.bp20.backend.api.ai.client.FastApiClient;
import com.bp20.backend.api.ai.dto.request.AgentRunResumeRequest;
import com.bp20.backend.global.exception.ApiException;
import com.bp20.backend.global.response.ApiResponse;
import com.bp20.backend.global.response.ErrorCode;
import com.bp20.backend.global.response.SuccessCode;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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

    private final FastApiClient fastApiClient;

    @PostMapping(value = "/analyses", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createAnalysis(
            @RequestParam MultipartFile file,
            @RequestParam("trdar_cd") @NotBlank String trdarCd,
            @RequestParam("svc_induty_cd") @NotBlank String svcIndutyCd,
            @RequestParam(value = "yyqu_cd", required = false) Integer yyquCd
    ) {
        validateCsv(file);
        return ApiResponse.success(
                SuccessCode.SUCCESS_AI_ANALYSIS_CREATE,
                fastApiClient.createAnalysis(file, trdarCd, svcIndutyCd, yyquCd)
        );
    }

    @GetMapping("/analyses/{analysisId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAnalysis(@PathVariable String analysisId) {
        return ApiResponse.success(
                SuccessCode.SUCCESS_AI_ANALYSIS_GET,
                fastApiClient.getAnalysis(analysisId)
        );
    }

    @PostMapping("/analyses/{analysisId}/recommendations")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createRecommendation(
            @PathVariable String analysisId
    ) {
        return ApiResponse.success(
                SuccessCode.SUCCESS_AI_RECOMMENDATION_CREATE,
                fastApiClient.createRecommendation(analysisId)
        );
    }

    @GetMapping("/agent-runs/{threadId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAgentRun(@PathVariable String threadId) {
        return ApiResponse.success(
                SuccessCode.SUCCESS_AI_AGENT_RUN_GET,
                fastApiClient.getAgentRun(threadId)
        );
    }

    @PostMapping("/agent-runs/{threadId}/resume")
    public ResponseEntity<ApiResponse<Map<String, Object>>> resumeAgentRun(
            @PathVariable String threadId,
            @Valid @RequestBody AgentRunResumeRequest request
    ) {
        return ApiResponse.success(
                SuccessCode.SUCCESS_AI_AGENT_RUN_RESUME,
                fastApiClient.resumeAgentRun(threadId, request)
        );
    }

    private void validateCsv(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".csv")) {
            throw new ApiException(ErrorCode.BAD_REQUEST_INVALID_FILE_EXTENSION);
        }
    }
}
