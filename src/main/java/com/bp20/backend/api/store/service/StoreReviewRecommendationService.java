package com.bp20.backend.api.store.service;

import com.bp20.backend.api.store.domain.StoreReviewRecommendation;
import com.bp20.backend.api.store.dto.response.RecommendationResponseDto;
import com.bp20.backend.api.store.repository.StoreReviewRecommendationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StoreReviewRecommendationService {

    private final StoreReviewRecommendationRepository storeReviewRecommendationRepository;

    @Transactional
    public StoreReviewRecommendation saveRecommendation(RecommendationResponseDto responseDto) {
        var report = responseDto.improvementReport();

        List<StoreReviewRecommendation.ActionItem> actionItems = report.actionItems()
                .stream()
                .map(item -> new StoreReviewRecommendation.ActionItem(
                        item.priority(),
                        item.aspect(),
                        item.keyword(),
                        item.trendSummary(),
                        item.problemCause(),
                        item.actionPlan(),
                        item.expectedOutcome()
                ))
                .toList();

        StoreReviewRecommendation recommendation = StoreReviewRecommendation.builder()
                .storeId(responseDto.storeId())
                .executiveSummary(report.executiveSummary())
                .actionItems(actionItems)
                .build();
        return storeReviewRecommendationRepository.save(recommendation);
    }
}
