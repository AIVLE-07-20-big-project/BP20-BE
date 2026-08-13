package com.bp20.backend.api.effectverification.collector;

import com.bp20.backend.api.effectverification.dto.request.PeriodMetrics;
import com.bp20.backend.api.effectverification.dto.request.RecommendationType;
import com.bp20.backend.api.effectverification.dto.request.VerificationCondition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ProductionVerificationMetricCollectorTests {

    private JdbcTemplate jdbcTemplate;
    private ProductionVerificationMetricCollector collector;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:production-verification;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
                "sa",
                ""
        );
        jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute("DROP ALL OBJECTS");
        jdbcTemplate.execute("""
                CREATE TABLE stores (
                    store_id BIGINT PRIMARY KEY,
                    owner_user_id BIGINT NOT NULL
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE csv_daily_sales (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    owner_id BIGINT NOT NULL,
                    sale_date DATE NOT NULL,
                    sales_amount BIGINT NOT NULL
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE orders (
                    order_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    store_id BIGINT NOT NULL,
                    total_amount BIGINT NOT NULL,
                    ordered_date DATE NOT NULL,
                    ordered_time TIME
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE coupons (
                    coupon_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    store_id BIGINT NOT NULL,
                    issued_at TIMESTAMP NOT NULL,
                    used_at TIMESTAMP
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE customers (
                    customer_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    store_id BIGINT NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE reviews (
                    review_id BIGINT PRIMARY KEY,
                    store_id BIGINT NOT NULL,
                    rating DOUBLE NOT NULL,
                    reviewed_date TIMESTAMP NOT NULL
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE review_analysis (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    review_id BIGINT NOT NULL,
                    aspect VARCHAR(50) NOT NULL,
                    sentiment VARCHAR(20) NOT NULL,
                    confidence DOUBLE
                )
                """);

        jdbcTemplate.update("INSERT INTO stores(store_id, owner_user_id) VALUES (?, ?)", 1L, 10L);
        jdbcTemplate.update(
                "INSERT INTO csv_daily_sales(owner_id, sale_date, sales_amount) VALUES (?, ?, ?)",
                10L, "2026-08-01", 30_000L
        );
        collector = new ProductionVerificationMetricCollector(jdbcTemplate);
    }

    @Test
    void collectsOrderCountAndAverageOrderValueForSalesVerification() {
        jdbcTemplate.update(
                "INSERT INTO orders(store_id, total_amount, ordered_date, ordered_time) VALUES (?, ?, ?, ?)",
                1L, 10_000L, "2026-08-01", "10:00:00"
        );
        jdbcTemplate.update(
                "INSERT INTO orders(store_id, total_amount, ordered_date, ordered_time) VALUES (?, ?, ?, ?)",
                1L, 20_000L, "2026-08-02", "15:00:00"
        );
        jdbcTemplate.update(
                "INSERT INTO orders(store_id, total_amount, ordered_date, ordered_time) VALUES (?, ?, ?, ?)",
                1L, 99_000L, "2026-08-03", "10:00:00"
        );
        jdbcTemplate.update(
                "INSERT INTO coupons(store_id, issued_at, used_at) VALUES (?, ?, ?)",
                1L, "2026-08-01 09:00:00", "2026-08-01 12:00:00"
        );
        jdbcTemplate.update(
                "INSERT INTO coupons(store_id, issued_at, used_at) VALUES (?, ?, ?)",
                1L, "2026-08-02 09:00:00", null
        );
        jdbcTemplate.update(
                "INSERT INTO customers(store_id, created_at) VALUES (?, ?)",
                1L, "2026-08-01 11:00:00"
        );
        jdbcTemplate.update(
                "INSERT INTO customers(store_id, created_at) VALUES (?, ?)",
                1L, "2026-08-02 11:00:00"
        );

        PeriodMetrics metrics = collector.collect(
                1L,
                RecommendationType.SALES,
                LocalDateTime.of(2026, 8, 1, 0, 0),
                LocalDateTime.of(2026, 8, 3, 0, 0),
                new VerificationCondition()
        );

        assertThat(metrics.getSales().getTargetSales()).isNull();
        assertThat(metrics.getSales().getVisitCount()).isEqualTo(2);
        assertThat(metrics.getSales().getAverageOrderValue()).isEqualTo(15_000.0);
        assertThat(metrics.getSales().getCouponUsageRate()).isEqualTo(50.0);
        assertThat(metrics.getSales().getNewCustomerCount()).isEqualTo(2);
        assertThat(metrics.getSales().getDormantCustomerReturnCount()).isNull();
        assertThat(metrics.getSales().getTotalSales()).isEqualTo(30_000.0);
    }

    @Test
    void countsKoreanAndEnglishNegativeSentiments() {
        jdbcTemplate.update(
                "INSERT INTO reviews(reveiw_id, store_id, rating, reviewed_date) VALUES (?, ?, ?, ?)",
                1L, 1L, 2.0, "2026-08-01 10:00:00"
        );
        jdbcTemplate.update(
                "INSERT INTO reviews(review_id, store_id, rating, reviewed_date) VALUES (?, ?, ?, ?)",
                2L, 1L, 4.0, "2026-08-02 10:00:00"
        );
        jdbcTemplate.update(
                "INSERT INTO review_analysis(review_id, aspect, sentiment, confidence) VALUES (?, ?, ?, ?)",
                1L, "food", "부정", 0.9
        );
        jdbcTemplate.update(
                "INSERT INTO review_analysis(review_id, aspect, sentiment, confidence) VALUES (?, ?, ?, ?)",
                2L, "food", "긍정", 0.8
        );
        jdbcTemplate.update(
                "INSERT INTO review_analysis(review_id, aspect, sentiment, confidence) VALUES (?, ?, ?, ?)",
                2L, "service", "negative", 0.7
        );

        PeriodMetrics metrics = collector.collect(
                1L,
                RecommendationType.REVIEW,
                LocalDateTime.of(2026, 8, 1, 0, 0),
                LocalDateTime.of(2026, 8, 3, 0, 0),
                new VerificationCondition(30, null, null, false, "food")
        );

        assertThat(metrics.getReview().getAverageRating()).isEqualTo(3.0);
        assertThat(metrics.getReview().getNegativeReviewRate()).isEqualTo(100.0);
        assertThat(metrics.getReview().getTargetAspectReviewCount()).isEqualTo(2);
        assertThat(metrics.getReview().getTargetAspectNegativeRate()).isEqualTo(50.0);
        assertThat(metrics.getReview().getTargetAspectAverageConfidence()).isEqualTo(0.85);
        assertThat(metrics.getReview().getReviewCount()).isEqualTo(2);
        assertThat(metrics.getReview().getSales()).isEqualTo(30_000.0);
    }
}
