package com.bp20.backend.api.effectverification.repository;

import com.bp20.backend.api.effectverification.domain.EffectVerificationExecution;
import com.bp20.backend.api.effectverification.domain.VerificationStatus;
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

    List<EffectVerificationExecution> findByStoreIdOrderByExecutedAtDesc(Long storeId);

    List<EffectVerificationExecution> findByStoreIdAndStatusOrderByExecutedAtDesc(
            Long storeId,
            VerificationStatus status
    );

    List<EffectVerificationExecution> findByStatusInAndVerificationDueAtLessThanEqualOrderByVerificationDueAtAsc(
            List<VerificationStatus> statuses,
            LocalDateTime dueAt
    );
}
