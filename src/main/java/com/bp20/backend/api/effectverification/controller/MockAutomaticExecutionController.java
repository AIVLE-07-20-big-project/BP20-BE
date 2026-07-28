package com.bp20.backend.api.effectverification.controller;

import com.bp20.backend.api.effectverification.dto.request.MockThreadExecutionRegistrationRequest;
import com.bp20.backend.api.effectverification.dto.response.VerificationExecutionResponse;
import com.bp20.backend.api.effectverification.dto.response.EffectVerificationResponse;
import com.bp20.backend.api.effectverification.service.MockAutomaticExecutionService;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    @PostMapping("/by-thread/{threadId}/register-auto")
    public ResponseEntity<VerificationExecutionResponse> registerThreadAutomatically(
            @PathVariable String threadId,
            @Valid @RequestBody MockThreadExecutionRegistrationRequest request
    ) {
        return ResponseEntity.ok(
                automaticExecutionService.registerThreadAutomatically(threadId, request)
        );
    }

    @PostMapping("/by-thread/{threadId}/complete-auto")
    public ResponseEntity<EffectVerificationResponse> completeThreadAutomatically(
            @PathVariable String threadId
    ) {
        return ResponseEntity.ok(
                automaticExecutionService.completeThreadAutomatically(threadId)
        );
    }

    @PostMapping("/reset")
    public ResponseEntity<MockAutomaticExecutionService.MockResetResult> reset() {
        return ResponseEntity.ok(
                automaticExecutionService.resetVerificationData()
        );
    }
}
