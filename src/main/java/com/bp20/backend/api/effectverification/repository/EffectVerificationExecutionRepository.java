package com.bp20.backend.api.effectverification.repository;

import com.bp20.backend.api.effectverification.domain.EffectVerificationExecution;
import com.bp20.backend.api.effectverification.domain.VerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.time.LocalDateTime;
import java.util.List;

public interface EffectVerificationExecutionRepository
        extends JpaRepository<EffectVerificationExecution, Long> {

    Optional<EffectVerificationExecution> findByAiRecommendationId(String aiRecommendationId);

    Optional<EffectVerificationExecution> findByAiRecommendationIdAndUser_Id(
            String aiRecommendationId,
            Long userId
    );

    boolean existsByAiRecommendationId(String aiRecommendationId);

    boolean existsByRecommendationRun_ThreadId(String threadId);

    boolean existsByDecisionId(String decisionId);

    Optional<EffectVerificationExecution> findByRecommendationRun_ThreadId(String threadId);

    Optional<EffectVerificationExecution> findByRecommendationRun_ThreadIdAndUser_Id(
            String threadId,
            Long userId
    );

    List<EffectVerificationExecution> findByStatusAndVerificationDueAtLessThanEqualOrderByVerificationDueAtAsc(
            VerificationStatus status,
            LocalDateTime dueAt
    );

    List<EffectVerificationExecution> findByStore_IdAndStatusAndVerificationDueAtLessThanEqualOrderByVerificationDueAtAsc(
            Long storeId,
            VerificationStatus status,
            LocalDateTime dueAt
    );

    List<EffectVerificationExecution> findByUser_IdAndStatusAndVerificationDueAtLessThanEqualOrderByVerificationDueAtAsc(
            Long userId,
            VerificationStatus status,
            LocalDateTime dueAt
    );

    List<EffectVerificationExecution> findByUser_IdAndStore_IdAndStatusAndVerificationDueAtLessThanEqualOrderByVerificationDueAtAsc(
            Long userId,
            Long storeId,
            VerificationStatus status,
            LocalDateTime dueAt
    );

    List<EffectVerificationExecution> findByStore_IdOrderByExecutedAtDesc(Long storeId);

    List<EffectVerificationExecution> findByUser_IdAndStore_IdOrderByExecutedAtDesc(
            Long userId,
            Long storeId
    );

    List<EffectVerificationExecution> findByStore_IdAndStatusOrderByExecutedAtDesc(
            Long storeId,
            VerificationStatus status
    );

    List<EffectVerificationExecution> findByUser_IdAndStore_IdAndStatusOrderByExecutedAtDesc(
            Long userId,
            Long storeId,
            VerificationStatus status
    );

    List<EffectVerificationExecution> findByStatusInAndVerificationDueAtLessThanEqualOrderByVerificationDueAtAsc(
            List<VerificationStatus> statuses,
            LocalDateTime dueAt
    );
}
