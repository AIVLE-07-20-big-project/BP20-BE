package com.bp20.backend.api.store.admin.service;

import com.bp20.backend.api.ai.domain.AiAnalysis;
import com.bp20.backend.api.ai.domain.AiRecommendationRun;
import com.bp20.backend.api.ai.repository.AiAnalysisRepository;
import com.bp20.backend.api.ai.repository.AiRecommendationRunRepository;
import com.bp20.backend.api.store.admin.dto.MerchantMonitoringResponse;
import com.bp20.backend.api.store.domain.Store;
import com.bp20.backend.api.store.repository.StoreRepository;
import com.bp20.backend.api.user.domain.UserStatus;
import com.bp20.backend.global.util.PersonalDataMasker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MerchantMonitoringService {

    private final StoreRepository storeRepository;
    private final AiAnalysisRepository analysisRepository;
    private final AiRecommendationRunRepository recommendationRunRepository;

    @Transactional(readOnly = true)
    public MerchantMonitoringResponse getMerchants() {
        List<Store> stores = storeRepository.findAll();
        Map<Long, List<AiAnalysis>> analysesByStore = analysisRepository.findAll().stream()
                .filter(analysis -> analysis.getStore() != null)
                .collect(Collectors.groupingBy(analysis -> analysis.getStore().getId()));
        Map<Long, List<AiRecommendationRun>> runsByStore = recommendationRunRepository.findAll().stream()
                .filter(run -> run.getAnalysis().getStore() != null)
                .collect(Collectors.groupingBy(run -> run.getAnalysis().getStore().getId()));

        List<MerchantMonitoringResponse.Merchant> merchants = stores.stream()
                .map(store -> toMerchant(store, analysesByStore, runsByStore))
                .toList();

        return new MerchantMonitoringResponse(
                stores.size(),
                merchants.stream().filter(merchant -> merchant.ownerStatus() == UserStatus.ACTIVE).count(),
                merchants.stream().filter(MerchantMonitoringResponse.Merchant::aiActive).count(),
                merchants
        );
    }

    private MerchantMonitoringResponse.Merchant toMerchant(
            Store store,
            Map<Long, List<AiAnalysis>> analysesByStore,
            Map<Long, List<AiRecommendationRun>> runsByStore
    ) {
        List<AiAnalysis> analyses = analysesByStore.getOrDefault(store.getId(), List.of());
        List<AiRecommendationRun> runs = runsByStore.getOrDefault(store.getId(), List.of());
        long executedRuns = runs.stream()
                .filter(run -> run.getExecutionStartedAt() != null || run.getExecutionEndedAt() != null)
                .count();

        return new MerchantMonitoringResponse.Merchant(
                store.getId(),
                store.getName(),
                store.getCategory(),
                store.getAddress(),
                store.getOwner().getId(),
                PersonalDataMasker.name(store.getOwner().getName()),
                PersonalDataMasker.email(store.getOwner().getEmail()),
                store.getOwner().getStatus(),
                store.getCreatedAt(),
                analyses.size(),
                runs.size(),
                executedRuns,
                !analyses.isEmpty()
        );
    }
}
