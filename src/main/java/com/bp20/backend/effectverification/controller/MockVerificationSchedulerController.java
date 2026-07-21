package com.bp20.backend.effectverification.controller;

import com.bp20.backend.effectverification.dto.response.SchedulerRunResponse;
import com.bp20.backend.effectverification.service.MockVerificationScheduler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("mock")
@RequestMapping("/api/mock/effect-verifications/scheduler")
@RequiredArgsConstructor
public class MockVerificationSchedulerController {

    private final MockVerificationScheduler scheduler;

    @PostMapping("/run")
    public ResponseEntity<SchedulerRunResponse> run() {
        return ResponseEntity.ok(scheduler.runDueVerifications());
    }
}
