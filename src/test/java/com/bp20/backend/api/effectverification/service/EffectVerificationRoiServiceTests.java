package com.bp20.backend.api.effectverification.service;

import com.bp20.backend.api.effectverification.domain.EffectVerificationResult;
import com.bp20.backend.api.effectverification.dto.request.RecommendationType;
import com.bp20.backend.api.effectverification.dto.response.EffectVerificationRoiResponse;
import com.bp20.backend.api.effectverification.repository.EffectVerificationResultRepository;
import com.bp20.backend.api.store.domain.Store;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class EffectVerificationRoiServiceTests {

    @Mock
    private EffectVerificationResultRepository resultRepository;

    @InjectMocks
    private EffectVerificationRoiService service;

    @Test
    void aggregatesOverallStoreTypeAndRecentStatistics() {
        List<EffectVerificationResult> results = List.of(
                result("r1", 1L, RecommendationType.SALES, 90.0,
                        "EFFECTIVE", LocalDateTime.of(2026, 7, 3, 0, 0)),
                result("r2", 1L, RecommendationType.REVIEW, 60.0,
                        "INCONCLUSIVE", LocalDateTime.of(2026, 7, 2, 0, 0)),
                result("r3", 2L, RecommendationType.SALES, 30.0,
                        "INEFFECTIVE", LocalDateTime.of(2026, 7, 1, 0, 0))
        );
        when(resultRepository.findAll()).thenReturn(results);

        EffectVerificationRoiResponse response = service.getSummary(null);

        assertThat(response.totalVerified()).isEqualTo(3);
        assertThat(response.averageEffectScore()).isEqualTo(60.0);
        assertThat(response.effectiveCount()).isEqualTo(1);
        assertThat(response.inconclusiveCount()).isEqualTo(1);
        assertThat(response.ineffectiveCount()).isEqualTo(1);
        assertThat(response.storeSummaries()).hasSize(2);
        assertThat(response.typeSummaries()).hasSize(2);
        assertThat(response.recentResults())
                .extracting(EffectVerificationRoiResponse.RecentResult::recommendationId)
                .containsExactly("r1", "r2", "r3");
    }

    @Test
    void filtersStatisticsByStore() {
        List<EffectVerificationResult> results = List.of(
                result("r1", 1L, RecommendationType.SALES, 80.0,
                        "EFFECTIVE", LocalDateTime.of(2026, 7, 2, 0, 0)),
                result("r2", 2L, RecommendationType.REVIEW, 20.0,
                        "NOT_EFFECTIVE", LocalDateTime.of(2026, 7, 1, 0, 0))
        );
        when(resultRepository.findAll()).thenReturn(results);

        EffectVerificationRoiResponse response = service.getSummary(1L);

        assertThat(response.totalVerified()).isEqualTo(1);
        assertThat(response.averageEffectScore()).isEqualTo(80.0);
        assertThat(response.effectiveCount()).isEqualTo(1);
        assertThat(response.storeSummaries())
                .extracting(EffectVerificationRoiResponse.StoreSummary::storeId)
                .containsExactly(1L);
    }

    @Test
    void returnsZeroValuesWhenThereAreNoResults() {
        when(resultRepository.findAll()).thenReturn(List.of());

        EffectVerificationRoiResponse response = service.getSummary(null);

        assertThat(response.totalVerified()).isZero();
        assertThat(response.averageEffectScore()).isZero();
        assertThat(response.storeSummaries()).isEmpty();
        assertThat(response.typeSummaries()).isEmpty();
        assertThat(response.recentResults()).isEmpty();
    }

    private EffectVerificationResult result(
            String recommendationId,
            Long storeId,
            RecommendationType type,
            double score,
            String verdict,
            LocalDateTime verifiedDate
    ) {
        Store store = mock(Store.class);
        when(store.getId()).thenReturn(storeId);
        return EffectVerificationResult.builder()
                .aiRecommendationId(recommendationId)
                .store(store)
                .recommendationType(type)
                .effectScore(score)
                .verdict(verdict)
                .metricResults("[]")
                .summary("")
                .verifiedDate(verifiedDate)
                .build();
    }
}
