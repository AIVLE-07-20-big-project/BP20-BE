package com.bp20.backend.api.ai.repository;

import com.bp20.backend.api.ai.domain.AiSalesFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiSalesFeedbackRepository extends JpaRepository<AiSalesFeedback, Long> {
    boolean existsByThreadId(String threadId);
}
