package com.bp20.backend.effectverification.repository;

import com.bp20.backend.effectverification.entity.EffectVerificationResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EffectVerificationResultRepository
        extends JpaRepository<EffectVerificationResult, Long> {

    Optional<EffectVerificationResult> findByAiRecommendationId(Long aiRecommendationId);

    boolean existsByAiRecommendationId(Long aiRecommendationId);
}