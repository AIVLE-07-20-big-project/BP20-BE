package com.bp20.backend.api.ai.repository;

import com.bp20.backend.api.ai.domain.AiRecommendationRun;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AiRecommendationRunRepository extends JpaRepository<AiRecommendationRun, String> {
    Optional<AiRecommendationRun> findByThreadIdAndUserId(String threadId, Long userId);
    List<AiRecommendationRun> findAllByUserIdOrderByCreatedAtDesc(Long userId);
    List<AiRecommendationRun> findAllByUserIdAndStoreIdOrderByCreatedAtDesc(Long userId, String storeId);
}
