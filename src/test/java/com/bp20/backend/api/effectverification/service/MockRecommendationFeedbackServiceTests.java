package com.bp20.backend.api.effectverification.service;

import com.bp20.backend.api.effectverification.dto.response.EffectVerificationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MockRecommendationFeedbackServiceTests {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private MockRecommendationFeedbackService service;

    @BeforeEach
    void setUp() {
        service = new MockRecommendationFeedbackService(jdbcTemplate);
    }

    @Test
    @SuppressWarnings("unchecked")
    void effectiveResultIncreasesStrategyWeight() {
        mockRecommendation(10001L);
        EffectVerificationResponse result = new EffectVerificationResponse();
        result.setEffectScore(85.0);
        result.setVerdict("EFFECTIVE");

        service.apply(10001L, result);

        verify(jdbcTemplate).update(
                anyString(),
                eq(1L),
                eq("REVISIT_COUPON"),
                eq(1.2),
                eq(85.0),
                eq("EFFECTIVE"),
                eq(0.2),
                eq(85.0),
                eq("EFFECTIVE")
        );
    }

    @Test
    void partiallyEffectiveResultKeepsStrategyWeight() {
        mockRecommendation(10002L);
        EffectVerificationResponse result = new EffectVerificationResponse();
        result.setEffectScore(55.0);
        result.setVerdict("PARTIALLY_EFFECTIVE");

        service.apply(10002L, result);

        verify(jdbcTemplate).update(
                anyString(),
                eq(1L),
                eq("REVISIT_COUPON"),
                eq(1.0),
                eq(55.0),
                eq("PARTIALLY_EFFECTIVE"),
                eq(0.0),
                eq(55.0),
                eq("PARTIALLY_EFFECTIVE")
        );
    }

    @Test
    void notEffectiveResultDecreasesStrategyWeight() {
        mockRecommendation(10003L);
        EffectVerificationResponse result = new EffectVerificationResponse();
        result.setEffectScore(25.0);
        result.setVerdict("NOT_EFFECTIVE");

        service.apply(10003L, result);

        verify(jdbcTemplate).update(
                anyString(),
                eq(1L),
                eq("REVISIT_COUPON"),
                eq(0.8),
                eq(25.0),
                eq("NOT_EFFECTIVE"),
                eq(-0.2),
                eq(25.0),
                eq("NOT_EFFECTIVE")
        );
    }

    @SuppressWarnings("unchecked")
    private void mockRecommendation(Long recommendationId) {
        when(jdbcTemplate.queryForObject(
                anyString(),
                any(RowMapper.class),
                eq(recommendationId)
        )).thenAnswer(invocation -> {
            RowMapper<?> mapper = invocation.getArgument(1);
            var resultSet = org.mockito.Mockito.mock(java.sql.ResultSet.class);
            when(resultSet.getLong("StoreID")).thenReturn(1L);
            when(resultSet.getString("ActionID")).thenReturn("REVISIT_COUPON");
            return mapper.mapRow(resultSet, 0);
        });
    }
}
