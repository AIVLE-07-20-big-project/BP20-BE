package com.bp20.backend.api.salestarget.dto.response;

import com.bp20.backend.api.salestarget.domain.PipelineStatus;
import com.bp20.backend.api.salestarget.domain.SalesTargetCandidate;

public record SalesTargetCandidateResponse(
        Long id,
        String name,
        String industry,
        String region,
        Double score,
        Double growthScore,
        Double trafficScore,
        Double reviewScore,
        Double similarityScore,
        String proposition,
        PipelineStatus pipelineStatus
) {
    public static SalesTargetCandidateResponse from(SalesTargetCandidate candidate) {
        return new SalesTargetCandidateResponse(
                candidate.getId(),
                candidate.getBusinessName(),
                candidate.getIndustry(),
                candidate.getAddress(),
                candidate.getTotalScore(),
                candidate.getGrowthScore(),
                candidate.getTrafficScore(),
                candidate.getReviewScore(),
                candidate.getSimilarityScore(),
                candidate.getSalesPitch(),
                candidate.getPipelineStatus()
        );
    }
}
