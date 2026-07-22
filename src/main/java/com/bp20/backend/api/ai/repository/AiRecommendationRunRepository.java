package com.bp20.backend.api.ai.repository;

import com.bp20.backend.api.ai.domain.AiRecommendationRun;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AiRecommendationRunRepository extends JpaRepository<AiRecommendationRun, String> {
    Optional<AiRecommendationRun> findByThreadIdAndUserId(String threadId, Long userId);
}
