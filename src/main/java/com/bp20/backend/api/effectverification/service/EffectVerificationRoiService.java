package com.bp20.backend.api.effectverification.service;

import com.bp20.backend.api.effectverification.domain.EffectVerificationResult;
import com.bp20.backend.api.effectverification.dto.request.RecommendationType;
import com.bp20.backend.api.effectverification.dto.response.EffectVerificationRoiResponse;
import com.bp20.backend.api.effectverification.repository.EffectVerificationResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EffectVerificationRoiService {

    private static final int RECENT_RESULT_LIMIT = 5;

    private final EffectVerificationResultRepository resultRepository;

    @Transactional(readOnly = true)
    public EffectVerificationRoiResponse getSummary(Long storeId) {
        List<EffectVerificationResult> results = resultRepository.findAll().stream()
                .filter(result -> storeId == null || storeId.equals(result.getStoreId()))
                .toList();

        long effectiveCount = results.stream()
                .filter(result -> "EFFECTIVE".equals(result.getVerdict()))
                .count();
        long ineffectiveCount = results.stream()
                .filter(result -> "INEFFECTIVE".equals(result.getVerdict())
                        || "NOT_EFFECTIVE".equals(result.getVerdict()))
                .count();
        long inconclusiveCount = results.size() - effectiveCount - ineffectiveCount;

        Map<Long, List<EffectVerificationResult>> byStore = results.stream()
                .collect(Collectors.groupingBy(EffectVerificationResult::getStoreId));
        List<EffectVerificationRoiResponse.StoreSummary> storeSummaries =
                byStore.entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .map(entry -> new EffectVerificationRoiResponse.StoreSummary(
                                entry.getKey(),
                                entry.getValue().size(),
                                averageScore(entry.getValue())
                        ))
                        .toList();

        Map<RecommendationType, List<EffectVerificationResult>> byType =
                results.stream().collect(Collectors.groupingBy(
                        EffectVerificationResult::getRecommendationType
                ));
        List<EffectVerificationRoiResponse.TypeSummary> typeSummaries =
                byType.entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .map(entry -> new EffectVerificationRoiResponse.TypeSummary(
                                entry.getKey(),
                                entry.getValue().size(),
                                averageScore(entry.getValue())
                        ))
                        .toList();

        List<EffectVerificationRoiResponse.RecentResult> recentResults =
                results.stream()
                        .sorted(Comparator.comparing(
                                EffectVerificationResult::getVerifiedDate
                        ).reversed())
                        .limit(RECENT_RESULT_LIMIT)
                        .map(result -> new EffectVerificationRoiResponse.RecentResult(
                                result.getAiRecommendationId(),
                                result.getStoreId(),
                                result.getRecommendationType(),
                                rounded(result.getEffectScore()),
                                result.getVerdict(),
                                result.getVerifiedDate()
                        ))
                        .toList();

        return new EffectVerificationRoiResponse(
                results.size(),
                averageScore(results),
                effectiveCount,
                inconclusiveCount,
                ineffectiveCount,
                storeSummaries,
                typeSummaries,
                recentResults
        );
    }

    private double averageScore(List<EffectVerificationResult> results) {
        if (results.isEmpty()) {
            return 0.0;
        }
        return rounded(results.stream()
                .mapToDouble(EffectVerificationResult::getEffectScore)
                .average()
                .orElse(0.0));
    }

    private double rounded(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
