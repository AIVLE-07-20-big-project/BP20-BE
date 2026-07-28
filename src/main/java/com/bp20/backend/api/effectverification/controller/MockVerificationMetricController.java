package com.bp20.backend.api.effectverification.controller;

import com.bp20.backend.api.effectverification.collector.VerificationMetricCollector;
import com.bp20.backend.api.effectverification.dto.request.PeriodMetrics;
import com.bp20.backend.api.effectverification.dto.request.RecommendationType;
import com.bp20.backend.api.effectverification.dto.request.VerificationCondition;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@Profile("mock")
@RequestMapping("/api/mock/effect-verification/metrics")
@RequiredArgsConstructor
public class MockVerificationMetricController {

    private final VerificationMetricCollector metricCollector;

    @GetMapping
    public ResponseEntity<PeriodMetrics> collect(
            @RequestParam(name = "store_id") Long storeId,
            @RequestParam RecommendationType type,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime to,
            @RequestParam(name = "start_hour", required = false) Integer startHour,
            @RequestParam(name = "end_hour", required = false) Integer endHour,
            @RequestParam(name = "target_aspect", required = false) String targetAspect
    ) {
        VerificationCondition condition = new VerificationCondition(
                null,
                startHour,
                endHour,
                true,
                targetAspect
        );
        return ResponseEntity.ok(
                metricCollector.collect(storeId, type, from, to, condition)
        );
    }
}

