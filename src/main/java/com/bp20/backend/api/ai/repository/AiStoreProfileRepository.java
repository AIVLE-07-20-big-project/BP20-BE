package com.bp20.backend.api.ai.repository;

import com.bp20.backend.api.ai.domain.AiStoreProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiStoreProfileRepository extends JpaRepository<AiStoreProfile, Long> {
}
