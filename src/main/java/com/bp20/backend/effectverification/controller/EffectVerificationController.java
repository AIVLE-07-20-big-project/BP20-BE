package com.bp20.backend.effectverification.controller;

import com.bp20.backend.effectverification.dto.request.EffectVerificationRequest;
import com.bp20.backend.effectverification.dto.response.EffectVerificationResponse;
import com.bp20.backend.effectverification.service.EffectVerificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/effect-verifications")
@RequiredArgsConstructor
public class EffectVerificationController {

    private final EffectVerificationService effectVerificationService;

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
