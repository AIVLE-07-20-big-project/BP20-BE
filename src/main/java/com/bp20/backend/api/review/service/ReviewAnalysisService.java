package com.bp20.backend.api.review.service;

import com.bp20.backend.api.review.domain.Review;
import com.bp20.backend.api.review.domain.ReviewAnalysis;
import com.bp20.backend.api.review.dto.*;
import com.bp20.backend.api.review.dto.request.BatchReviewRequestDto;
import com.bp20.backend.api.review.dto.response.ABSAAgentResponseDto;
import com.bp20.backend.api.review.dto.response.AspectRadarResponseDto;
import com.bp20.backend.api.review.dto.response.AspectStatResponseDto;

import java.time.LocalDateTime;
import java.util.ArrayList;

import com.bp20.backend.api.review.repository.ReviewAnalysisRepository;
import com.bp20.backend.api.review.repository.ReviewRepository;
import com.bp20.backend.api.store.domain.Store;
import com.bp20.backend.api.store.domain.StoreReviewKeyword;
import com.bp20.backend.api.store.domain.StoreReviewRecommendation;
import com.bp20.backend.api.store.repository.StoreRepository;
import com.bp20.backend.api.store.repository.StoreReviewKeywordRepository;
import com.bp20.backend.api.store.repository.StoreReviewRecommendationRepository;
import lombok.extern.slf4j.Slf4j;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Service
public class ReviewAnalysisService {

    private final ReviewAnalysisRepository reviewAnalysisRepository;
    private final ReviewRepository reviewRepository;
    private final StoreReviewKeywordRepository storeReviewKeywordRepository;
    private final StoreReviewRecommendationRepository storeReviewRecommendationRepository;
    private final StoreRepository storeRepository;
    private final WebClient webClient;

    public ReviewAnalysisService(
            ReviewAnalysisRepository reviewAnalysisRepository,
            ReviewRepository reviewRepository,
            StoreReviewKeywordRepository storeReviewKeywordRepository,
            StoreReviewRecommendationRepository storeReviewRecommendationRepository,
            StoreRepository storeRepository,
            @Value("${app.fastapi.base-url}") String baseUrl
    ) {
        this.reviewAnalysisRepository = reviewAnalysisRepository;
        this.reviewRepository = reviewRepository;
        this.storeReviewKeywordRepository = storeReviewKeywordRepository;
        this.storeReviewRecommendationRepository = storeReviewRecommendationRepository;
        this.storeRepository = storeRepository;
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
    }

    @Transactional
    public void analyzeUnanalyzedReviews(Long storeId) {
        List<Review> unanalyzedReviews =
                reviewRepository.findTop30ByStore_IdAndIsAnalyzedFalse(storeId);

        if (unanalyzedReviews.isEmpty())
            return;

        List<ReviewItemDto> requestDtos = unanalyzedReviews.stream()
                .map(review -> new ReviewItemDto(review.getId(), review.getContent()))
                .toList();

        analyzeAndSaveReviews(storeId, requestDtos);
    }

    public void analyzeAndSaveReviews(Long storeId, List<ReviewItemDto> requestDtos) {
        try {
            BatchReviewRequestDto requestDto = new BatchReviewRequestDto(storeId, requestDtos);

            Store store = storeRepository.getReferenceById(storeId);

            ABSAAgentResponseDto response = webClient.post()
                    .uri("/api/v1/review/analyze-graph")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestDto)
                    .retrieve()
                    .bodyToMono(ABSAAgentResponseDto.class)
                    .block();

            if (response == null) return;

            List<ReviewAnalysis> analysisEntitiesToSave = new ArrayList<>();

            if (response.reviewsAnalysis() != null) {
                for (ReviewAnalysisDto res : response.reviewsAnalysis()) {
                    if (res.reviewId() == null) continue;
                    reviewRepository.findById(res.reviewId()).ifPresent(review -> {
                        review.markAsAnalyzed();

                        for (AspectSentimentDto result : res.results()) {
                            ReviewAnalysis entity = ReviewAnalysis.builder()
                                    .review(review)
                                    .aspect(result.aspect())
                                    .sentiment(result.sentiment())
                                    .confidence(result.confidence())
                                    .build();
                            analysisEntitiesToSave.add(entity);
                        }
                    });
                }
            }
            List<StoreReviewKeyword> keywordEntitiesToSave = new ArrayList<>();

            if (response.clusters() != null) {
                for (KeywordClusterItemDto cluster : response.clusters()) {
                    StoreReviewKeyword keywordEntity = StoreReviewKeyword.builder()
                            .store(store)
                            .aspect(cluster.aspect())
                            .sentiment(cluster.sentiment())
                            .keyword(cluster.representativeKeyword())
                            .count(cluster.count())
                            .analyzedAt(LocalDateTime.now())
                            .build();
                    keywordEntitiesToSave.add(keywordEntity);
                }
            }

            reviewAnalysisRepository.saveAll(analysisEntitiesToSave);
            storeReviewKeywordRepository.saveAll(keywordEntitiesToSave);

            if (response.improvementReport() != null) {
                var report = response.improvementReport();

                List<StoreReviewRecommendation.ActionItem> actionItems = report.actionItems().stream()
                        .map(item -> new StoreReviewRecommendation.ActionItem(
                                item.priority(),
                                item.aspect(),
                                item.keyword(),
                                item.trendSummary(),
                                item.problemCause(),
                                item.actionPlan(),
                                item.expectedOutcome(),
                                null
                        ))
                        .toList();

                StoreReviewRecommendation recommendation = StoreReviewRecommendation.builder()
                        .store(store)
                        .executiveSummary(report.executiveSummary())
                        .actionItems(actionItems)
                        .build();

                storeReviewRecommendationRepository.save(recommendation);
            }
            log.info("FastAPI Agent 분석 완료 (storeId: {}): 개별 리뷰 {}건, 키워드 클러스터 {}건 저장 성공",
                    response.storeId(), analysisEntitiesToSave.size(), keywordEntitiesToSave.size());

        } catch (org.springframework.web.reactive.function.client.WebClientResponseException e) {
            log.error("FastAPI 응답 에러 (Status: {}): {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("AI 분석 서비스 통신 실패", e);
        } catch (Exception e) {
            log.error("FastAPI 분석 요청 중 예외 발생: {}", e.getMessage(), e);
            throw new RuntimeException("AI 분석 서비스 처리 실패", e);
        }
    }
    
    @Transactional(readOnly = true)
    @Cacheable(value = "aspectScores", key = "#storeId")
    public List<AspectRadarResponseDto> getAspectRadarChart(Long storeId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfCurrentMonth = now.withDayOfMonth(1).toLocalDate().atStartOfDay();
        LocalDateTime startOfPrevMonth = startOfCurrentMonth.minusMonths(1);

        List<AspectScoreDto> currentScores =
                reviewAnalysisRepository.findAspectScoresByStoreAndDate(storeId, startOfCurrentMonth, now);
        List<AspectScoreDto> prevScores =
                reviewAnalysisRepository.findAspectScoresByStoreAndDate(storeId, startOfPrevMonth, startOfCurrentMonth);

        Map<String, Double> prevScoreMap = prevScores.stream()
                .collect(Collectors.toMap(AspectScoreDto::getAspect, AspectScoreDto::getScore));

        return currentScores.stream()
                .map(current -> new AspectRadarResponseDto(
                        current.getAspect(),
                        current.getScore(),
                        prevScoreMap.getOrDefault(current.getAspect(), 0.0)
                ))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AspectStatResponseDto> getAspectStats(Long storeId) {
        return reviewAnalysisRepository.findAspectStatsByStoreId(storeId);
    }
}
