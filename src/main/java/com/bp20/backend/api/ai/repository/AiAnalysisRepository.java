package com.bp20.backend.api.ai.repository;

import com.bp20.backend.api.ai.domain.AiAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AiAnalysisRepository extends JpaRepository<AiAnalysis, String> {
    Optional<AiAnalysis> findByAnalysisIdAndUserId(String analysisId, Long userId);
}
