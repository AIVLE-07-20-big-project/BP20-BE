package com.bp20.backend.api.review.service;

import com.bp20.backend.api.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Runs one 30-review analysis batch for each store that has enough pending reviews.
 * Manual analysis remains available for stores with fewer than 30 pending reviews.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewAnalysisScheduler {

    private static final int BATCH_SIZE = 30;

    private final ReviewRepository reviewRepository;
    private final ReviewAnalysisService reviewAnalysisService;

    @Value("${review-analysis.scheduler.enabled:true}")
    private boolean enabled;

    @Scheduled(
            fixedDelayString = "${review-analysis.scheduler.fixed-delay-ms:60000}",
            initialDelayString = "${review-analysis.scheduler.initial-delay-ms:60000}"
    )
    public void runScheduled() {
        if (!enabled) {
            return;
        }

        List<Long> storeIds = reviewRepository
                .findStoreIdsWithAtLeastUnanalyzedReviews(BATCH_SIZE);

        for (Long storeId : storeIds) {
            try {
                reviewAnalysisService.analyzeUnanalyzedReviews(storeId);
            } catch (RuntimeException exception) {
                log.warn(
                        "Automatic review analysis failed for store {}: {}",
                        storeId,
                        exception.getMessage()
                );
            }
        }
    }
}
