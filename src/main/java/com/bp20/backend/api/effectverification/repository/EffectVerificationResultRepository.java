package com.bp20.backend.api.effectverification.repository;

import com.bp20.backend.api.effectverification.domain.EffectVerificationResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EffectVerificationResultRepository
        extends JpaRepository<EffectVerificationResult, Long> {

    Optional<EffectVerificationResult> findByAiRecommendationId(String aiRecommendationId);

    Optional<EffectVerificationResult> findByAiRecommendationIdAndUser_Id(
            String aiRecommendationId,
            Long userId
    );

    boolean existsByAiRecommendationId(String aiRecommendationId);
}
