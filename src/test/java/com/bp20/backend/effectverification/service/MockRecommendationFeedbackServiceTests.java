package com.bp20.backend.effectverification.service;

import com.bp20.backend.effectverification.dto.response.EffectVerificationResponse;
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
        when(jdbcTemplate.queryForObject(
                anyString(),
                any(RowMapper.class),
                eq(10001L)
        )).thenAnswer(invocation -> {
            RowMapper<?> mapper = invocation.getArgument(1);
            var resultSet = org.mockito.Mockito.mock(java.sql.ResultSet.class);
            when(resultSet.getLong("StoreID")).thenReturn(1L);
            when(resultSet.getString("ActionID")).thenReturn("REVISIT_COUPON");
            return mapper.mapRow(resultSet, 0);
        });
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
}
