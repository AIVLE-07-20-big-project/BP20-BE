package com.bp20.backend.api.salestarget.service;

import com.bp20.backend.api.salestarget.domain.PipelineStatus;
import com.bp20.backend.api.salestarget.domain.SalesTargetCandidate;
import com.bp20.backend.api.salestarget.dto.request.BulkUpsertSalesTargetsRequest;
import com.bp20.backend.api.salestarget.dto.request.SalesTargetItemRequest;
import com.bp20.backend.api.salestarget.dto.response.SalesTargetCandidateResponse;
import com.bp20.backend.api.salestarget.repository.SalesTargetCandidateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * 신규 가맹점 영업 타겟 후보의 조회/상태변경(관리자용)과, AI 서버 배치 결과 반영(벌크 업서트,
 * 내부용)을 담당한다.
 *
 * 벌크 업서트 설계: 이번 배치에 없는 기존 후보(예: 이번엔 상위 N위 밖으로 밀려난 경우)는
 * 삭제하지 않는다. 영업팀이 이미 CONTACT_PLANNED 이상으로 진행시킨 후보가 다음 배치에서
 * 우연히 순위가 밀렸다고 사라지면 안 되기 때문이다 — 대신 자연키(businessName+address)로
 * 이미 존재하는 후보를 찾으면 점수만 갱신하고 pipelineStatus는 절대 건드리지 않는다
 * (SalesTargetCandidate.refreshScores 참고). 새로 나타난 후보만 CANDIDATE 상태로 새로 만든다.
 *
 * 트레이드오프: 이번 배치의 상위 N위 안에 더 이상 들지 못하는 오래된 CANDIDATE 상태 후보가
 * 테이블에 계속 남는다. 관리자가 수동으로 EXCLUDED 처리하기 전까지는 목록에서 안 사라진다 —
 * 지금 단계에서는 "이미 진행 중인 영업을 실수로 지우지 않는 것"을 "목록을 항상 깨끗하게
 * 유지하는 것"보다 더 중요하게 판단했다.
 */
@Service
@RequiredArgsConstructor
public class SalesTargetService {

    private final SalesTargetCandidateRepository salesTargetCandidateRepository;

    @Transactional(readOnly = true)
    public List<SalesTargetCandidateResponse> getAll(PipelineStatus statusFilter) {
        List<SalesTargetCandidate> candidates = statusFilter == null
                ? salesTargetCandidateRepository.findAllByOrderByTotalScoreDesc()
                : salesTargetCandidateRepository.findAllByPipelineStatusOrderByTotalScoreDesc(statusFilter);
        return candidates.stream().map(SalesTargetCandidateResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public SalesTargetCandidateResponse getById(Long id) {
        return SalesTargetCandidateResponse.from(findOrThrow(id));
    }

    @Transactional
    public SalesTargetCandidateResponse updatePipelineStatus(Long id, PipelineStatus newStatus) {
        SalesTargetCandidate candidate = findOrThrow(id);
        candidate.updatePipelineStatus(newStatus);
        return SalesTargetCandidateResponse.from(candidate);
    }

    @Transactional
    public BulkUpsertResult bulkUpsertCandidates(BulkUpsertSalesTargetsRequest request) {
        int created = 0;
        int updated = 0;

        for (SalesTargetItemRequest item : request.items()) {
            Optional<SalesTargetCandidate> existing = salesTargetCandidateRepository
                    .findByBusinessNameAndAddress(item.businessName(), item.address());

            if (existing.isPresent()) {
                existing.get().refreshScores(
                        item.industry(),
                        item.totalScore(),
                        item.growthScore(),
                        item.trafficScore(),
                        item.reviewScore(),
                        item.similarityScore(),
                        request.sourceBatchId()
                );
                updated++;
            } else {
                SalesTargetCandidate candidate = SalesTargetCandidate.builder()
                        .businessName(item.businessName())
                        .industry(item.industry())
                        .address(item.address())
                        .totalScore(item.totalScore())
                        .growthScore(item.growthScore())
                        .trafficScore(item.trafficScore())
                        .reviewScore(item.reviewScore())
                        .similarityScore(item.similarityScore())
                        .pipelineStatus(PipelineStatus.CANDIDATE)
                        .sourceBatchId(request.sourceBatchId())
                        .build();
                salesTargetCandidateRepository.save(candidate);
                created++;
            }
        }

        return new BulkUpsertResult(created, updated);
    }

    private SalesTargetCandidate findOrThrow(Long id) {
        return salesTargetCandidateRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("존재하지 않는 영업 타겟입니다: id=" + id));
    }

    public record BulkUpsertResult(int created, int updated) {
    }
}
