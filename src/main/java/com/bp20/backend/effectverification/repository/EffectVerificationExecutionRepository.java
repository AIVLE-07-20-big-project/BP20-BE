package com.bp20.backend.effectverification.repository;

import com.bp20.backend.effectverification.entity.EffectVerificationExecution;
import com.bp20.backend.effectverification.entity.VerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.time.LocalDateTime;
import java.util.List;

public interface EffectVerificationExecutionRepository
        extends JpaRepository<EffectVerificationExecution, Long> {

    Optional<EffectVerificationExecution> findByAiRecommendationId(Long aiRecommendationId);

    boolean existsByAiRecommendationId(Long aiRecommendationId);

    List<EffectVerificationExecution> findByStatusAndVerificationDueAtLessThanEqualOrderByVerificationDueAtAsc(
            VerificationStatus status,
            LocalDateTime dueAt
    );

    List<EffectVerificationExecution> findByStoreIdAndStatusAndVerificationDueAtLessThanEqualOrderByVerificationDueAtAsc(
            Long storeId,
            VerificationStatus status,
            LocalDateTime dueAt
    );
}
