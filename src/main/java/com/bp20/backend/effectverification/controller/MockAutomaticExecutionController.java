package com.bp20.backend.effectverification.controller;

import com.bp20.backend.effectverification.dto.response.VerificationExecutionResponse;
import com.bp20.backend.effectverification.dto.response.EffectVerificationResponse;
import com.bp20.backend.effectverification.service.MockAutomaticExecutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("mock")
@RequestMapping("/api/mock/effect-verifications/executions")
@RequiredArgsConstructor
public class MockAutomaticExecutionController {

    private final MockAutomaticExecutionService automaticExecutionService;

    @PostMapping("/{recommendationId}/register-auto")
    public ResponseEntity<VerificationExecutionResponse> registerAutomatically(
            @PathVariable Long recommendationId
    ) {
        return ResponseEntity.ok(
                automaticExecutionService.registerAutomatically(recommendationId)
        );
    }

    @PostMapping("/{recommendationId}/complete-auto")
    public ResponseEntity<EffectVerificationResponse> completeAutomatically(
            @PathVariable Long recommendationId
    ) {
        return ResponseEntity.ok(
                automaticExecutionService.completeAutomatically(recommendationId)
        );
    }
}
