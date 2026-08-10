package com.bp20.backend.api.store.service;

import com.bp20.backend.api.store.domain.Store;
import com.bp20.backend.api.store.domain.StoreReviewRecommendation;
import com.bp20.backend.api.store.dto.response.FastAPIRecommendationResponseDto;
import com.bp20.backend.api.store.dto.response.StoreRecommendationResponseDto;
import com.bp20.backend.api.store.repository.StoreRepository;
import com.bp20.backend.api.store.repository.StoreReviewRecommendationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StoreReviewRecommendationService {

    private final StoreReviewRecommendationRepository storeReviewRecommendationRepository;
    private final StoreRepository storeRepository;

    @Transactional
    public StoreReviewRecommendation saveRecommendation(FastAPIRecommendationResponseDto responseDto) {
        var report = responseDto.improvementReport();

        Store store = storeRepository.getReferenceById(responseDto.storeId());

        List<StoreReviewRecommendation.ActionItem> actionItems = report.actionItems()
                .stream()
                .map(item -> new StoreReviewRecommendation.ActionItem(
                        item.priority(),
                        item.aspect(),
                        item.keyword(),
                        item.trendSummary(),
                        item.problemCause(),
                        item.actionPlan(),
                        item.expectedOutcome(),
                        item.executedAt()
                ))
                .toList();

        StoreReviewRecommendation recommendation = StoreReviewRecommendation.builder()
                .store(store)
                .executiveSummary(report.executiveSummary())
                .actionItems(actionItems)
                .reportMonth(null)
                .build();
        return storeReviewRecommendationRepository.save(recommendation);
    }

    @Transactional(readOnly = true)
    public StoreRecommendationResponseDto getLatestRecommendation(Long storeId) {
        StoreReviewRecommendation recommendation = storeReviewRecommendationRepository.findTopByStore_IdOrderByCreatedAtDesc(storeId)
                .orElseThrow(() -> new IllegalArgumentException("해당 매장의 AI 추천 내용이 없습니다"));

        List<StoreRecommendationResponseDto.ActionItemDto> actionItemDtos = recommendation.getActionItems()
                .stream()
                .map(item -> new StoreRecommendationResponseDto.ActionItemDto(
                        item.priority(),
                        item.aspect(),
                        item.keyword(),
                        item.trendSummary(),
                        item.problemCause(),
                        item.actionPlan(),
                        item.expectedOutcome(),
                        item.executedAt()
                ))
                .toList();
        return new StoreRecommendationResponseDto(
                recommendation.getId(),
                recommendation.getStore().getId(),
                recommendation.getExecutiveSummary(),
                actionItemDtos,
                recommendation.getCreatedAt()
        );
    }

    @Transactional
    public CompletedAction completeActionItem(Long recommendationId, String keyword) {
        StoreReviewRecommendation recommendation = storeReviewRecommendationRepository.findById(recommendationId)
                .orElseThrow(() -> new IllegalArgumentException("해당 추천사항이 없습니다."));

        List<StoreReviewRecommendation.ActionItem> actionItems = recommendation.getActionItems();
        int actionIndex = -1;
        for (int index = 0; index < actionItems.size(); index++) {
            if (actionItems.get(index).keyword().equals(keyword)) {
                actionIndex = index;
                break;
            }
        }
        if (actionIndex < 0) {
            throw new IllegalArgumentException("해당 키워드의 추천 실행 항목이 없습니다.");
        }

        StoreReviewRecommendation.ActionItem actionItem = actionItems.get(actionIndex);
        LocalDateTime executedAt = actionItem.executedAt() == null
                ? LocalDateTime.now()
                : actionItem.executedAt();
        boolean newlyCompleted = actionItem.executedAt() == null;
        List<StoreReviewRecommendation.ActionItem> updateActionItems =
                new java.util.ArrayList<>(actionItems);
        updateActionItems.set(actionIndex, new StoreReviewRecommendation.ActionItem(
                actionItem.priority(),
                actionItem.aspect(),
                actionItem.keyword(),
                actionItem.trendSummary(),
                actionItem.problemCause(),
                actionItem.actionPlan(),
                actionItem.expectedOutcome(),
                executedAt
        ));

        recommendation.setActionItems(updateActionItems);

        storeReviewRecommendationRepository.save(recommendation);
        return new CompletedAction(
                "review-" + recommendationId + "-" + (actionIndex + 1),
                recommendation.getStore().getId(),
                actionItem.aspect(),
                actionItem.actionPlan(),
                executedAt,
                newlyCompleted
        );
    }

    public record CompletedAction(
            String verificationRecommendationId,
            Long storeId,
            String targetAspect,
            String actionPlan,
            LocalDateTime executedAt,
            boolean newlyCompleted
    ) {
    }
}
