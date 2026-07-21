package com.bp20.backend.effectverification.service;

import com.bp20.backend.effectverification.client.ReviewSentimentApiClient;
import com.bp20.backend.effectverification.dto.response.AspectSentimentResponse;
import com.bp20.backend.effectverification.dto.response.ReviewAnalysisStorageResponse;
import com.bp20.backend.effectverification.dto.response.ReviewSentimentResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MockReviewSentimentServiceTests {

    @Mock
    private ReviewSentimentApiClient sentimentApiClient;

    @Mock
    private JdbcTemplate jdbcTemplate;

    private MockReviewSentimentService service;

    @BeforeEach
    void setUp() {
        service = new MockReviewSentimentService(sentimentApiClient, jdbcTemplate);
    }

    @Test
    void analyzeAndStorePreservesConfidenceAndNormalizesThreeSentiments() {
        when(jdbcTemplate.queryForObject(anyString(), eq(String.class), eq(1L)))
                .thenReturn("맛은 좋고 가격은 보통이지만 서비스는 불친절해요.");
        when(sentimentApiClient.analyze(anyString())).thenReturn(
                new ReviewSentimentResponse(
                        "맛은 좋고 가격은 보통이지만 서비스는 불친절해요.",
                        List.of(
                                new AspectSentimentResponse("food", "긍정", 92.5),
                                new AspectSentimentResponse("price", "중립", 88.0),
                                new AspectSentimentResponse("service", "부정", 97.3)
                        )
                )
        );

        ReviewAnalysisStorageResponse response = service.analyzeAndStore(1L);

        assertThat(response.results()).hasSize(3);
        assertThat(response.results().get(0).sentimentScore()).isEqualTo(0.925);
        assertThat(response.results().get(1).sentimentScore()).isZero();
        assertThat(response.results().get(2).sentimentScore()).isEqualTo(-0.973);
        verify(jdbcTemplate).update(
                "UPDATE MockReview SET Negative = ? WHERE ReviewID = ?",
                true,
                1L
        );
    }

    @Test
    void analyzePendingProcessesOnlyReviewsWithoutStoredResults() {
        LocalDateTime from = LocalDateTime.of(2026, 6, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 6, 15, 0, 0);
        when(jdbcTemplate.queryForList(
                anyString(),
                eq(Long.class),
                eq(1L),
                any(),
                any()
        )).thenReturn(List.of(15L));
        when(jdbcTemplate.queryForObject(anyString(), eq(String.class), eq(15L)))
                .thenReturn("서비스는 보통이었어요");
        when(sentimentApiClient.analyze(anyString())).thenReturn(
                new ReviewSentimentResponse(
                        "서비스는 보통이었어요",
                        List.of(new AspectSentimentResponse(
                                "service",
                                "중립",
                                90.0
                        ))
                )
        );

        var response = service.analyzePending(1L, from, to);

        assertThat(response.analyzedCount()).isEqualTo(1);
        assertThat(response.results().getFirst().reviewId()).isEqualTo(15L);
        assertThat(response.results().getFirst().results().getFirst().sentimentScore())
                .isZero();
    }
}
