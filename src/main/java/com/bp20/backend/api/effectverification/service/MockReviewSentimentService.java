package com.bp20.backend.api.effectverification.service;

import com.bp20.backend.api.effectverification.client.ReviewSentimentApiClient;
import com.bp20.backend.api.effectverification.dto.response.AspectSentimentResponse;
import com.bp20.backend.api.effectverification.dto.response.ReviewAnalysisStorageResponse;
import com.bp20.backend.api.effectverification.dto.response.ReviewBatchAnalysisResponse;
import com.bp20.backend.api.effectverification.dto.response.ReviewSentimentResponse;
import com.bp20.backend.api.effectverification.dto.response.StoredAspectSentimentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.time.LocalDateTime;
import java.sql.Timestamp;

@Service
@Profile("mock")
@RequiredArgsConstructor
public class MockReviewSentimentService {

    private final ReviewSentimentApiClient sentimentApiClient;
    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public ReviewAnalysisStorageResponse analyzeAndStore(Long reviewId) {
        String reviewText = findReviewText(reviewId);
        ReviewSentimentResponse analysis = sentimentApiClient.analyze(reviewText);
        List<StoredAspectSentimentResponse> storedResults = analysis.results()
                .stream()
                .map(this::normalize)
                .toList();

        jdbcTemplate.update(
                "DELETE FROM MockReviewAspectSentiment WHERE ReviewID = ?",
                reviewId
        );
        for (StoredAspectSentimentResponse result : storedResults) {
            jdbcTemplate.update(
                    """
                    INSERT INTO MockReviewAspectSentiment
                        (ReviewID, Aspect, Sentiment, Confidence, SentimentScore)
                    VALUES (?, ?, ?, ?, ?)
                    """,
                    reviewId,
                    result.aspect(),
                    result.sentiment(),
                    result.confidence(),
                    result.sentimentScore()
            );
        }

        boolean negative = storedResults.stream()
                .anyMatch(result -> "부정".equals(result.sentiment()));
        jdbcTemplate.update(
                "UPDATE MockReview SET Negative = ? WHERE ReviewID = ?",
                negative,
                reviewId
        );

        return new ReviewAnalysisStorageResponse(
                reviewId,
                analysis.reviewText(),
                storedResults
        );
    }

    @Transactional
    public ReviewBatchAnalysisResponse analyzePending(
            Long storeId,
            LocalDateTime from,
            LocalDateTime to
    ) {
        if (!from.isBefore(to)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "from must be before to"
            );
        }

        List<Long> reviewIds = jdbcTemplate.queryForList(
                """
                SELECT review.ReviewID
                FROM MockReview review
                WHERE review.StoreID = ?
                  AND review.CreatedAt >= ? AND review.CreatedAt < ?
                  AND NOT EXISTS (
                      SELECT 1 FROM MockReviewAspectSentiment sentiment
                      WHERE sentiment.ReviewID = review.ReviewID
                  )
                ORDER BY review.CreatedAt ASC, review.ReviewID ASC
                """,
                Long.class,
                storeId,
                Timestamp.valueOf(from),
                Timestamp.valueOf(to)
        );
        List<ReviewAnalysisStorageResponse> results = reviewIds.stream()
                .map(this::analyzeAndStore)
                .toList();
        return new ReviewBatchAnalysisResponse(results.size(), results);
    }

    private String findReviewText(Long reviewId) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT Content FROM MockReview WHERE ReviewID = ?",
                    String.class,
                    reviewId
            );
        } catch (EmptyResultDataAccessException exception) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Mock review not found"
            );
        }
    }

    private StoredAspectSentimentResponse normalize(AspectSentimentResponse result) {
        if (result.aspect() == null || result.sentiment() == null
                || result.confidence() == null) {
            throw new IllegalStateException(
                    "Review sentiment AI returned an incomplete result"
            );
        }
        if (result.confidence() < 0 || result.confidence() > 100) {
            throw new IllegalStateException(
                    "Review sentiment confidence must be between 0 and 100"
            );
        }

        String sentiment = result.sentiment().trim();
        double normalizedConfidence = result.confidence() / 100.0;
        double sentimentScore = switch (sentiment.toLowerCase(Locale.ROOT)) {
            case "긍정", "positive" -> normalizedConfidence;
            case "중립", "neutral" -> 0.0;
            case "부정", "negative" -> -normalizedConfidence;
            default -> throw new IllegalStateException(
                    "Unsupported sentiment label: " + result.sentiment()
            );
        };

        return new StoredAspectSentimentResponse(
                result.aspect(),
                sentiment,
                result.confidence(),
                Math.round(sentimentScore * 10_000.0) / 10_000.0
        );
    }
}
