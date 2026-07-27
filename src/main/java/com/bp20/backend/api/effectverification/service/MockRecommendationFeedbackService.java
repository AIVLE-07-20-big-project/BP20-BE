package com.bp20.backend.api.effectverification.service;

import com.bp20.backend.api.effectverification.dto.response.EffectVerificationResponse;
import com.bp20.backend.api.effectverification.dto.response.RecommendationStrategyWeightResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Profile("mock")
@RequiredArgsConstructor
public class MockRecommendationFeedbackService {

    private static final double DEFAULT_WEIGHT = 1.0;

    private final JdbcTemplate jdbcTemplate;

    public void apply(
            Long recommendationId,
            EffectVerificationResponse verification
    ) {
        RecommendationAction action = jdbcTemplate.queryForObject(
                """
                SELECT StoreID, ActionID
                FROM MockRecommendation
                WHERE RecommendationID = ?
                """,
                (resultSet, rowNumber) -> new RecommendationAction(
                        resultSet.getLong("StoreID"),
                        resultSet.getString("ActionID")
                ),
                recommendationId
        );
        if (action == null) {
            throw new IllegalStateException("Mock recommendation action not found");
        }

        double delta = weightDelta(verification.getVerdict());
        jdbcTemplate.update(
                """
                INSERT INTO MockRecommendationStrategyWeight
                    (StoreID, ActionID, Weight, LastEffectScore, LastVerdict, UpdatedAt)
                VALUES (?, ?, ?, ?, ?, NOW())
                ON DUPLICATE KEY UPDATE
                    Weight = LEAST(2.0, GREATEST(0.1, Weight + ?)),
                    LastEffectScore = ?,
                    LastVerdict = ?,
                    UpdatedAt = NOW()
                """,
                action.storeId(),
                action.actionId(),
                clamp(DEFAULT_WEIGHT + delta),
                verification.getEffectScore(),
                verification.getVerdict(),
                delta,
                verification.getEffectScore(),
                verification.getVerdict()
        );
    }

    public List<RecommendationStrategyWeightResponse> getByStore(Long storeId) {
        return jdbcTemplate.query(
                """
                SELECT StoreID, ActionID, Weight, LastEffectScore,
                       LastVerdict, UpdatedAt
                FROM MockRecommendationStrategyWeight
                WHERE StoreID = ?
                ORDER BY Weight DESC, ActionID ASC
                """,
                (resultSet, rowNumber) -> new RecommendationStrategyWeightResponse(
                        resultSet.getLong("StoreID"),
                        resultSet.getString("ActionID"),
                        resultSet.getDouble("Weight"),
                        resultSet.getDouble("LastEffectScore"),
                        resultSet.getString("LastVerdict"),
                        resultSet.getTimestamp("UpdatedAt").toLocalDateTime()
                ),
                storeId
        );
    }

    private double weightDelta(String verdict) {
        if (verdict == null) {
            return 0.0;
        }
        return switch (verdict) {
            case "EFFECTIVE" -> 0.2;
            case "NOT_EFFECTIVE" -> -0.2;
            case "PARTIALLY_EFFECTIVE" -> 0.0;
            default -> 0.0;
        };
    }

    private double clamp(double value) {
        return Math.max(0.1, Math.min(2.0, value));
    }

    private record RecommendationAction(Long storeId, String actionId) {
    }
}
