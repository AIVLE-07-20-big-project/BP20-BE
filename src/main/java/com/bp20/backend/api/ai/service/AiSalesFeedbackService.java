package com.bp20.backend.api.ai.service;

import com.bp20.backend.api.ai.client.FastApiClient;
import com.bp20.backend.api.ai.domain.AiRecommendationRun;
import com.bp20.backend.api.ai.domain.AiSalesFeedback;
import com.bp20.backend.api.ai.repository.AiRecommendationRunRepository;
import com.bp20.backend.api.ai.repository.AiSalesFeedbackRepository;
import com.bp20.backend.api.csv.domain.CsvDailySales;
import com.bp20.backend.api.csv.repository.CsvDailySalesRepository;
import com.bp20.backend.api.effectverification.dto.response.EffectVerificationResponse;
import com.bp20.backend.api.effectverification.dto.response.MetricResult;
import com.bp20.backend.api.effectverification.service.EffectVerificationLifecycleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AiSalesFeedbackService {
    private final AiRecommendationRunRepository runRepository;
    private final AiSalesFeedbackRepository feedbackRepository;
    private final CsvDailySalesRepository salesRepository;
    private final FastApiClient fastApiClient;
    private final EffectVerificationLifecycleService effectVerificationLifecycleService;

    /** 새 매출 CSV 저장 후 실행 기간이 끝난 승인 방안의 전후 매출을 학습자료로 저장한다. */
    @Transactional
    public int processUploadedSales(Long userId, LocalDate uploadedFrom, LocalDate uploadedToExclusive) {
        List<AiRecommendationRun> runs = runRepository.findAllByUser_IdOrderByCreatedAtDesc(userId);
        int processed = 0;
        for (AiRecommendationRun run : runs) {
            if (run.getExecutionStartedAt() == null || run.getExecutionEndedAt() == null
                    || run.getExecutionEndedAt().toLocalDate().isAfter(uploadedToExclusive.minusDays(1))
                    || run.getExecutionEndedAt().toLocalDate().isBefore(uploadedFrom)
                    || feedbackRepository.existsByThreadId(run.getThreadId())) {
                continue;
            }

            LocalDate beforeFrom = run.getExecutionStartedAt().toLocalDate()
                    .minusDays(java.time.Duration.between(run.getExecutionStartedAt(), run.getExecutionEndedAt()).toDays());
            LocalDate beforeTo = run.getExecutionStartedAt().toLocalDate();
            LocalDate afterFrom = run.getExecutionStartedAt().toLocalDate();
            LocalDate afterTo = run.getExecutionEndedAt().toLocalDate().plusDays(1);

            double before = sumSales(userId, beforeFrom, beforeTo);
            double after = sumSales(userId, afterFrom, afterTo);
            double rate = before == 0.0 ? 0.0 : ((after - before) / before) * 100.0;

            AiSalesFeedback feedback = feedbackRepository.save(AiSalesFeedback.create(
                    run.getThreadId(), userId, run.getSelectedAction(),
                    run.getExecutionStartedAt(), run.getExecutionEndedAt(),
                    before, after, Math.round(rate * 100.0) / 100.0
            ));
            int measurementDays = Math.max(1, (int) java.time.Duration.between(
                    run.getExecutionStartedAt(), run.getExecutionEndedAt()).toDays());
            try {
                effectVerificationLifecycleService.completeSalesVerificationFromCsv(
                        userId, run.getThreadId(), after, run.getExecutionEndedAt());
            } catch (RuntimeException ignored) {
                // 검증 실행이 등록되지 않은 과거 추천도 AI 학습자료에는 남긴다.
            }
            try {
                fastApiClient.recordSalesFeedback(
                        run.getThreadId(), before, after, measurementDays, userId);
                feedback.markLearned();
                feedbackRepository.save(feedback);
            } catch (RuntimeException ignored) {
                // DB의 실측값은 보존하고, AI 서비스가 복구되면 별도 재처리할 수 있다.
            }
            processed++;
        }
        return processed;
    }

    /** 매출형 전략검증(analysis_id 비교)을 완료하고, 그 결과(대상 시간대 매출 변화)로 밴딧 모델도 온라인 학습시킨다. */
    @Transactional
    public EffectVerificationResponse completeVerificationFromAnalysisAndLearn(
            Long userId, String threadId, String afterAnalysisId
    ) {
        EffectVerificationResponse response = effectVerificationLifecycleService
                .completeVerificationFromAnalyses(userId, threadId, afterAnalysisId);
        feedSalesFeedback(userId, threadId, response);
        return response;
    }

    private void feedSalesFeedback(Long userId, String threadId, EffectVerificationResponse response) {
        // 같은 thread가 일별 매출 CSV 경로로 이미 학습자료를 남겼다면 중복 기록하지 않는다
        // (thread_id가 유니크 제약이라 그대로 저장하면 실패한다).
        if (feedbackRepository.existsByThreadId(threadId)) {
            return;
        }
        List<MetricResult> metricResults = response.getMetricResults();
        MetricResult target = (metricResults == null ? List.<MetricResult>of() : metricResults).stream()
                .filter(metric -> "target_sales".equals(metric.getMetricName()))
                .findFirst()
                .orElse(null);
        if (target == null || target.getBeforeValue() == null || target.getAfterValue() == null) {
            return;
        }
        AiRecommendationRun run = runRepository.findByThreadIdAndUser_Id(threadId, userId).orElse(null);
        if (run == null) {
            return;
        }

        double before = target.getBeforeValue();
        double after = target.getAfterValue();
        double rate = before == 0.0 ? 0.0 : ((after - before) / before) * 100.0;
        LocalDateTime executionStartedAt = run.getExecutionStartedAt() != null
                ? run.getExecutionStartedAt() : LocalDateTime.now();
        LocalDateTime executionEndedAt = run.getExecutionEndedAt() != null
                ? run.getExecutionEndedAt() : LocalDateTime.now();
        int measurementDays = (int) Math.max(1, Duration.between(executionStartedAt, executionEndedAt).toDays());

        AiSalesFeedback feedback = feedbackRepository.save(AiSalesFeedback.create(
                threadId, userId, run.getSelectedAction(),
                executionStartedAt, executionEndedAt,
                before, after, Math.round(rate * 100.0) / 100.0
        ));
        try {
            fastApiClient.recordSalesFeedback(threadId, before, after, measurementDays, userId);
            feedback.markLearned();
            feedbackRepository.save(feedback);
        } catch (RuntimeException ignored) {
            // 검증 결과 자체는 이미 저장됐으니, 학습 반영 실패로 전체 요청을 막지 않는다.
        }
    }

    private double sumSales(Long userId, LocalDate from, LocalDate toExclusive) {
        return salesRepository
                .findByOwner_IdAndSaleDateGreaterThanEqualAndSaleDateLessThan(userId, from, toExclusive)
                .stream()
                .mapToDouble(CsvDailySales::getSalesAmount)
                .sum();
    }
}
