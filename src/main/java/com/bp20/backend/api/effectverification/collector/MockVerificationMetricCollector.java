package com.bp20.backend.api.effectverification.collector;

import com.bp20.backend.api.effectverification.dto.request.PeriodMetrics;
import com.bp20.backend.api.effectverification.dto.request.RecommendationType;
import com.bp20.backend.api.effectverification.dto.request.ReviewMetrics;
import com.bp20.backend.api.effectverification.dto.request.SalesMetrics;
import com.bp20.backend.api.effectverification.dto.request.VerificationCondition;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@Component
@Profile("mock")
@RequiredArgsConstructor
public class MockVerificationMetricCollector implements VerificationMetricCollector {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public PeriodMetrics collect(
            Long storeId,
            RecommendationType recommendationType,
            LocalDateTime from,
            LocalDateTime to,
            VerificationCondition condition
    ) {
        validatePeriod(from, to);
        return switch (recommendationType) {
            case SALES -> new PeriodMetrics(
                    collectSales(storeId, from, to, condition),
                    null
            );
            case REVIEW -> new PeriodMetrics(
                    null,
                    collectReview(storeId, from, to, condition)
            );
        };
    }

    private SalesMetrics collectSales(
            Long storeId,
            LocalDateTime from,
            LocalDateTime to,
            VerificationCondition condition
    ) {
        int startHour = condition.getStartHour() == null ? 0 : condition.getStartHour();
        int endHour = condition.getEndHour() == null ? 24 : condition.getEndHour();
        Object[] periodParams = periodParams(storeId, from, to);
        Object[] targetParams = {
                storeId,
                Timestamp.valueOf(from),
                Timestamp.valueOf(to),
                startHour,
                endHour
        };

        Double targetSales = number(
                """
                SELECT COALESCE(SUM(TotalAmount), 0)
                FROM MockOrders
                WHERE StoreID = ? AND OrderedAt >= ? AND OrderedAt < ?
                  AND HOUR(OrderedAt) >= ? AND HOUR(OrderedAt) < ?
                """,
                Double.class,
                targetParams
        );
        Integer visitCount = number(
                """
                SELECT COUNT(*) FROM MockVisit
                WHERE StoreID = ? AND VisitedAt >= ? AND VisitedAt < ?
                  AND HOUR(VisitedAt) >= ? AND HOUR(VisitedAt) < ?
                """,
                Integer.class,
                targetParams
        );
        Double averageOrderValue = number(
                """
                SELECT COALESCE(AVG(TotalAmount), 0)
                FROM MockOrders
                WHERE StoreID = ? AND OrderedAt >= ? AND OrderedAt < ?
                  AND HOUR(OrderedAt) >= ? AND HOUR(OrderedAt) < ?
                """,
                Double.class,
                targetParams
        );

        return new SalesMetrics(
                targetSales,
                visitCount,
                averageOrderValue,
                calculateRevisitRate(storeId, from, to),
                calculateCouponUsageRate(storeId, from, to),
                number(
                        "SELECT COUNT(*) FROM MockCustomer WHERE StoreID = ? AND JoinedAt >= ? AND JoinedAt < ?",
                        Integer.class,
                        periodParams
                ),
                calculateDormantReturns(storeId, from, to),
                number(
                        "SELECT COALESCE(SUM(TotalAmount), 0) FROM MockOrders WHERE StoreID = ? AND OrderedAt >= ? AND OrderedAt < ?",
                        Double.class,
                        periodParams
                )
        );
    }

    private ReviewMetrics collectReview(
            Long storeId,
            LocalDateTime from,
            LocalDateTime to,
            VerificationCondition condition
    ) {
        Object[] periodParams = periodParams(storeId, from, to);
        String aspect = condition.getTargetAspect();
        Object[] aspectParams = {
                storeId,
                Timestamp.valueOf(from),
                Timestamp.valueOf(to),
                aspect
        };

        return new ReviewMetrics(
                number(
                        "SELECT COALESCE(AVG(Rating), 0) FROM MockReview WHERE StoreID = ? AND CreatedAt >= ? AND CreatedAt < ?",
                        Double.class,
                        periodParams
                ),
                percentage(
                        """
                        SELECT COUNT(*) FROM MockReview review
                        WHERE review.StoreID = ? AND review.CreatedAt >= ? AND review.CreatedAt < ?
                          AND EXISTS (
                              SELECT 1 FROM MockReviewAspectSentiment sentiment
                              WHERE sentiment.ReviewID = review.ReviewID
                                AND sentiment.Sentiment IN ('부정', 'negative')
                          )
                        """,
                        periodParams,
                        "SELECT COUNT(*) FROM MockReview WHERE StoreID = ? AND CreatedAt >= ? AND CreatedAt < ?",
                        periodParams
                ),
                number(
                        """
                        SELECT COUNT(*)
                        FROM MockReviewAspectSentiment sentiment
                        JOIN MockReview review ON review.ReviewID = sentiment.ReviewID
                        WHERE review.StoreID = ? AND review.CreatedAt >= ? AND review.CreatedAt < ?
                          AND sentiment.Aspect = ?
                        """,
                        Integer.class,
                        aspectParams
                ),
                percentage(
                        """
                        SELECT COUNT(*)
                        FROM MockReviewAspectSentiment sentiment
                        JOIN MockReview review ON review.ReviewID = sentiment.ReviewID
                        WHERE review.StoreID = ? AND review.CreatedAt >= ? AND review.CreatedAt < ?
                          AND sentiment.Aspect = ?
                          AND sentiment.Sentiment IN ('부정', 'negative')
                        """,
                        aspectParams,
                        """
                        SELECT COUNT(*)
                        FROM MockReviewAspectSentiment sentiment
                        JOIN MockReview review ON review.ReviewID = sentiment.ReviewID
                        WHERE review.StoreID = ? AND review.CreatedAt >= ? AND review.CreatedAt < ?
                          AND sentiment.Aspect = ?
                        """,
                        aspectParams
                ),
                number(
                        """
                        SELECT COALESCE(AVG(sentiment.Confidence) / 100, 0)
                        FROM MockReviewAspectSentiment sentiment
                        JOIN MockReview review ON review.ReviewID = sentiment.ReviewID
                        WHERE review.StoreID = ? AND review.CreatedAt >= ? AND review.CreatedAt < ?
                          AND sentiment.Aspect = ?
                        """,
                        Double.class,
                        aspectParams
                ),
                number(
                        "SELECT COUNT(*) FROM MockReview WHERE StoreID = ? AND CreatedAt >= ? AND CreatedAt < ?",
                        Integer.class,
                        periodParams
                ),
                calculateRevisitRate(storeId, from, to),
                number(
                        "SELECT COALESCE(SUM(TotalAmount), 0) FROM MockOrders WHERE StoreID = ? AND OrderedAt >= ? AND OrderedAt < ?",
                        Double.class,
                        periodParams
                )
        );
    }

    private Double calculateRevisitRate(Long storeId, LocalDateTime from, LocalDateTime to) {
        Object[] params = periodParams(storeId, from, to);
        return percentage(
                """
                SELECT COUNT(*) FROM (
                    SELECT CustomerID FROM MockVisit
                    WHERE StoreID = ? AND VisitedAt >= ? AND VisitedAt < ?
                    GROUP BY CustomerID HAVING COUNT(*) >= 2
                ) repeat_customers
                """,
                params,
                "SELECT COUNT(DISTINCT CustomerID) FROM MockVisit WHERE StoreID = ? AND VisitedAt >= ? AND VisitedAt < ?",
                params
        );
    }

    private Double calculateCouponUsageRate(Long storeId, LocalDateTime from, LocalDateTime to) {
        Object[] params = periodParams(storeId, from, to);
        return percentage(
                "SELECT COUNT(*) FROM MockCouponIssue WHERE StoreID = ? AND IssuedAt >= ? AND IssuedAt < ? AND UsedAt IS NOT NULL",
                params,
                "SELECT COUNT(*) FROM MockCouponIssue WHERE StoreID = ? AND IssuedAt >= ? AND IssuedAt < ?",
                params
        );
    }

    private Integer calculateDormantReturns(Long storeId, LocalDateTime from, LocalDateTime to) {
        return number(
                """
                SELECT COUNT(DISTINCT current_visit.CustomerID)
                FROM MockVisit current_visit
                JOIN MockCustomer customer ON customer.CustomerID = current_visit.CustomerID
                WHERE current_visit.StoreID = ?
                  AND current_visit.VisitedAt >= ? AND current_visit.VisitedAt < ?
                  AND customer.JoinedAt < ?
                  AND NOT EXISTS (
                      SELECT 1 FROM MockVisit previous_visit
                      WHERE previous_visit.CustomerID = current_visit.CustomerID
                        AND previous_visit.VisitedAt >= ?
                        AND previous_visit.VisitedAt < ?
                  )
                """,
                Integer.class,
                storeId,
                Timestamp.valueOf(from),
                Timestamp.valueOf(to),
                Timestamp.valueOf(from),
                Timestamp.valueOf(from.minusDays(90)),
                Timestamp.valueOf(from)
        );
    }

    private Double percentage(
            String numeratorSql,
            Object[] numeratorParams,
            String denominatorSql,
            Object[] denominatorParams
    ) {
        Integer numerator = number(numeratorSql, Integer.class, numeratorParams);
        Integer denominator = number(denominatorSql, Integer.class, denominatorParams);
        if (denominator == 0) {
            return 0.0;
        }
        return Math.round((numerator * 10_000.0 / denominator)) / 100.0;
    }

    private <T extends Number> T number(String sql, Class<T> type, Object... params) {
        T value = jdbcTemplate.queryForObject(sql, type, params);
        if (value == null) {
            throw new IllegalStateException("Metric query returned null");
        }
        return value;
    }

    private Object[] periodParams(Long storeId, LocalDateTime from, LocalDateTime to) {
        return new Object[]{storeId, Timestamp.valueOf(from), Timestamp.valueOf(to)};
    }

    private void validatePeriod(LocalDateTime from, LocalDateTime to) {
        if (!from.isBefore(to)) {
            throw new IllegalArgumentException("from must be before to");
        }
    }
}
