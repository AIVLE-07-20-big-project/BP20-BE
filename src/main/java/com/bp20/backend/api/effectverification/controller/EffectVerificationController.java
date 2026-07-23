package com.bp20.backend.api.effectverification.controller;

import com.bp20.backend.api.effectverification.dto.request.EffectVerificationRequest;
import com.bp20.backend.api.effectverification.dto.request.ExecutionRegistrationRequest;
import com.bp20.backend.api.effectverification.dto.request.VerificationCompletionRequest;
import com.bp20.backend.api.effectverification.dto.response.EffectVerificationResponse;
import com.bp20.backend.api.effectverification.dto.response.VerificationExecutionResponse;
import com.bp20.backend.api.effectverification.domain.VerificationStatus;
import com.bp20.backend.api.effectverification.service.EffectVerificationLifecycleService;
import com.bp20.backend.api.effectverification.service.EffectVerificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/effect-verifications")
@RequiredArgsConstructor
public class EffectVerificationController {

    private final EffectVerificationService effectVerificationService;
    private final EffectVerificationLifecycleService lifecycleService;

    @PostMapping("/executions")
    public ResponseEntity<VerificationExecutionResponse> registerExecution(
            @Valid @RequestBody ExecutionRegistrationRequest request
    ) {
        return ResponseEntity.ok(lifecycleService.registerExecution(request));
    }

    @PostMapping("/executions/{recommendationId}/complete")
    public ResponseEntity<EffectVerificationResponse> completeVerification(
            @PathVariable Long recommendationId,
            @Valid @RequestBody VerificationCompletionRequest request
    ) {
        return ResponseEntity.ok(
                lifecycleService.completeVerification(recommendationId, request)
        );
    }

    @GetMapping("/executions/{recommendationId}")
    public ResponseEntity<VerificationExecutionResponse> getExecution(
            @PathVariable Long recommendationId
    ) {
        return ResponseEntity.ok(lifecycleService.getExecution(recommendationId));
    }

    @GetMapping("/executions/due")
    public ResponseEntity<List<VerificationExecutionResponse>> getDueExecutions(
            @RequestParam(name = "store_id", required = false) Long storeId
    ) {
        return ResponseEntity.ok(lifecycleService.getDueExecutions(storeId));
    }

    @GetMapping("/executions")
    public ResponseEntity<List<VerificationExecutionResponse>> getExecutionHistory(
            @RequestParam(name = "store_id") Long storeId,
            @RequestParam(name = "status", required = false) VerificationStatus status
    ) {
        return ResponseEntity.ok(
                lifecycleService.getExecutionHistory(storeId, status)
        );
    }

    @PostMapping("/verify")
    public ResponseEntity<EffectVerificationResponse> verifyEffect(
            @Valid @RequestBody EffectVerificationRequest request
    ) {
        EffectVerificationResponse response =
                effectVerificationService.verifyEffect(request);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/recommendations/{recommendationId}")
    public ResponseEntity<EffectVerificationResponse> getByRecommendationId(
            @PathVariable Long recommendationId
    ) {
        return ResponseEntity.ok(
                effectVerificationService.getByRecommendationId(recommendationId)
        );
    }
}
