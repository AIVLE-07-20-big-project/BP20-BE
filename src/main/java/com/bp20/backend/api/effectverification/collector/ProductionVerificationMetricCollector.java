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
@Profile("!mock")
@RequiredArgsConstructor
public class ProductionVerificationMetricCollector
        implements VerificationMetricCollector {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public PeriodMetrics collect(
            Long storeId,
            RecommendationType recommendationType,
            LocalDateTime from,
            LocalDateTime to,
            VerificationCondition condition
    ) {
        if (!from.isBefore(to)) {
            throw new IllegalArgumentException("from must be before to");
        }
        return switch (recommendationType) {
            case SALES -> new PeriodMetrics(
                    collectSales(storeId, from, to),
                    null
            );
            case REVIEW -> new PeriodMetrics(
                    null,
                    collectReview(storeId, from, to, condition.getTargetAspect())
            );
        };
    }

    private SalesMetrics collectSales(
            Long storeId,
            LocalDateTime from,
            LocalDateTime to
    ) {
        return new SalesMetrics(
                null,
                null,
                null,
                null,
                percentage(
                        """
                        SELECT COUNT(*)
                        FROM coupons
                        WHERE store_id = ? AND issued_at >= ? AND issued_at < ?
                          AND used_at IS NOT NULL
                        """,
                        """
                        SELECT COUNT(*)
                        FROM coupons
                        WHERE store_id = ? AND issued_at >= ? AND issued_at < ?
                        """,
                        storeId,
                        from,
                        to
                ),
                integer(
                        """
                        SELECT COUNT(*)
                        FROM customers
                        WHERE store_id = ? AND created_at >= ? AND created_at < ?
                        """,
                        storeId,
                        from,
                        to
                ),
                null,
                collectTotalSales(storeId, from, to)
        );
    }

    private ReviewMetrics collectReview(
            Long storeId,
            LocalDateTime from,
            LocalDateTime to,
            String targetAspect
    ) {
        Object[] period = periodParams(storeId, from, to);
        Object[] aspectPeriod = {
                storeId,
                Timestamp.valueOf(from),
                Timestamp.valueOf(to),
                targetAspect
        };
        return new ReviewMetrics(
                decimal(
                        """
                        SELECT AVG(rating)
                        FROM reviews
                        WHERE store_id = ? AND reviewed_date >= ? AND reviewed_date < ?
                        """,
                        period
                ),
                percentage(
                        """
                        SELECT COUNT(DISTINCT r.id)
                        FROM reviews r
                        JOIN review_analysis ra ON ra.review_id = r.id
                        WHERE r.store_id = ? AND r.reviewed_date >= ? AND r.reviewed_date < ?
                          AND (LOWER(ra.sentiment) = 'negative' OR ra.sentiment = '부정')
                        """,
                        """
                        SELECT COUNT(*)
                        FROM reviews
                        WHERE store_id = ? AND reviewed_date >= ? AND reviewed_date < ?
                        """,
                        storeId,
                        from,
                        to
                ),
                integer(
                        """
                        SELECT COUNT(*)
                        FROM review_analysis ra
                        JOIN reviews r ON r.id = ra.review_id
                        WHERE r.store_id = ? AND r.reviewed_date >= ? AND r.reviewed_date < ?
                          AND ra.aspect = ?
                        """,
                        aspectPeriod
                ),
                percentage(
                        """
                        SELECT COUNT(*)
                        FROM review_analysis ra
                        JOIN reviews r ON r.id = ra.review_id
                        WHERE r.store_id = ? AND r.reviewed_date >= ? AND r.reviewed_date < ?
                          AND ra.aspect = ?
                          AND (LOWER(ra.sentiment) = 'negative' OR ra.sentiment = '부정')
                        """,
                        """
                        SELECT COUNT(*)
                        FROM review_analysis ra
                        JOIN reviews r ON r.id = ra.review_id
                        WHERE r.store_id = ? AND r.reviewed_date >= ? AND r.reviewed_date < ?
                          AND ra.aspect = ?
                        """,
                        aspectPeriod
                ),
                decimal(
                        """
                        SELECT AVG(ra.confidence)
                        FROM review_analysis ra
                        JOIN reviews r ON r.id = ra.review_id
                        WHERE r.store_id = ? AND r.reviewed_date >= ? AND r.reviewed_date < ?
                          AND ra.aspect = ?
                        """,
                        aspectPeriod
                ),
                integer(
                        """
                        SELECT COUNT(*)
                        FROM reviews
                        WHERE store_id = ? AND reviewed_date >= ? AND reviewed_date < ?
                        """,
                        period
                ),
                null,
                collectTotalSales(storeId, from, to)
        );
    }

    private Double collectTotalSales(
            Long storeId,
            LocalDateTime from,
            LocalDateTime to
    ) {
        return decimal(
                """
                SELECT SUM(sales.sales_amount)
                FROM csv_daily_sales sales
                JOIN stores store ON store.owner_user_id = sales.owner_id
                WHERE store.store_id = ?
                  AND sales.sale_date >= ? AND sales.sale_date < ?
                """,
                storeId,
                from.toLocalDate(),
                to.toLocalDate()
        );
    }

    private Double percentage(
            String numeratorSql,
            String denominatorSql,
            Long storeId,
            LocalDateTime from,
            LocalDateTime to
    ) {
        Object[] params = periodParams(storeId, from, to);
        return percentage(numeratorSql, denominatorSql, params);
    }

    private Double percentage(
            String numeratorSql,
            String denominatorSql,
            Object[] params
    ) {
        Integer numerator = integer(numeratorSql, params);
        Integer denominator = integer(denominatorSql, params);
        if (denominator == null || denominator == 0) {
            return null;
        }
        return Math.round(numerator * 10_000.0 / denominator) / 100.0;
    }

    private Integer integer(String sql, Object... params) {
        Number value = jdbcTemplate.queryForObject(sql, Number.class, params);
        return value == null ? null : value.intValue();
    }

    private Double decimal(String sql, Object... params) {
        Number value = jdbcTemplate.queryForObject(sql, Number.class, params);
        return value == null ? null : value.doubleValue();
    }

    private Object[] periodParams(
            Long storeId,
            LocalDateTime from,
            LocalDateTime to
    ) {
        return new Object[]{
                storeId,
                Timestamp.valueOf(from),
                Timestamp.valueOf(to)
        };
    }
}
