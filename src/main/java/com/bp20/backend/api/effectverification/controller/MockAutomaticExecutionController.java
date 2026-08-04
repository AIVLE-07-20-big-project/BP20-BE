package com.bp20.backend.api.effectverification.controller;

import com.bp20.backend.api.effectverification.dto.request.MockThreadExecutionRegistrationRequest;
import com.bp20.backend.api.effectverification.dto.request.RecommendationType;
import com.bp20.backend.api.effectverification.dto.request.SelectedActionRequest;
import com.bp20.backend.api.effectverification.dto.response.VerificationExecutionResponse;
import com.bp20.backend.api.effectverification.dto.response.EffectVerificationResponse;
import com.bp20.backend.api.effectverification.dto.response.MockVerificationCandidateResponse;
import com.bp20.backend.api.effectverification.service.MockAutomaticExecutionService;
import com.bp20.backend.global.security.principal.SecurityPrincipal;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@Profile("mock")
@RequestMapping("/api/mock/effect-verifications/executions")
@RequiredArgsConstructor
public class MockAutomaticExecutionController {

    private static final String MOCK_APPROVED_THREAD =
            "mock-approved-sales-thread";
    private static final String MOCK_APPROVED_REVIEW_THREAD =
            "mock-approved-review-thread";

    private final MockAutomaticExecutionService automaticExecutionService;

    @GetMapping("/candidates")
    public ResponseEntity<List<MockVerificationCandidateResponse>> candidates() {
        List<MockVerificationCandidateResponse> candidates = new ArrayList<>();

        if (!automaticExecutionService.isThreadRegistered(MOCK_APPROVED_THREAD)) {
            SelectedActionRequest selectedAction = new SelectedActionRequest();
            selectedAction.setAction("재방문 쿠폰 발행");
            selectedAction.setAxis("discount_coupon");

            candidates.add(new MockVerificationCandidateResponse(
                    MOCK_APPROVED_THREAD,
                    1L,
                    RecommendationType.SALES,
                    null,
                    "approved",
                    selectedAction,
                    Map.of(
                            "report", "재방문 쿠폰 발행을 권장합니다.",
                            "verified", true
                    ),
                    LocalDateTime.now(),
                    true
            ));
        }

        if (!automaticExecutionService.isThreadRegistered(
                MOCK_APPROVED_REVIEW_THREAD
        )) {
            SelectedActionRequest selectedAction = new SelectedActionRequest();
            selectedAction.setAction("대기시간 개선");
            selectedAction.setAxis("review_improvement");

            candidates.add(new MockVerificationCandidateResponse(
                    MOCK_APPROVED_REVIEW_THREAD,
                    1L,
                    RecommendationType.REVIEW,
                    "convenience",
                    "approved",
                    selectedAction,
                    Map.of(
                            "report", "대기시간 관련 운영 개선을 권장합니다.",
                            "verified", true
                    ),
                    LocalDateTime.now(),
                    true
            ));
        }

        return ResponseEntity.ok(candidates);
    }

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
            @AuthenticationPrincipal SecurityPrincipal currentUser,
            @Valid @RequestBody MockThreadExecutionRegistrationRequest request
    ) {
        return ResponseEntity.ok(
                automaticExecutionService.registerThreadAutomatically(
                        currentUser == null ? null : currentUser.id(),
                        threadId,
                        request
                )
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
