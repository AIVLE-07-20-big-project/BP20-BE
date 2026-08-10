package com.bp20.backend.api.salestarget.repository;

import com.bp20.backend.api.salestarget.domain.PipelineStatus;
import com.bp20.backend.api.salestarget.domain.SalesTargetCandidate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SalesTargetCandidateRepository extends JpaRepository<SalesTargetCandidate, Long> {

    List<SalesTargetCandidate> findAllByOrderByTotalScoreDesc();

    List<SalesTargetCandidate> findAllByPipelineStatusOrderByTotalScoreDesc(PipelineStatus pipelineStatus);

    // 벌크 업서트 시 "이미 존재하는 후보인지" 확인하는 자연키 조회.
    Optional<SalesTargetCandidate> findByBusinessNameAndAddress(String businessName, String address);
}
