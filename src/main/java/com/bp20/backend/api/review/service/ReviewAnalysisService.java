package com.bp20.backend.api.review.service;

import com.bp20.backend.api.review.domain.Review;
import com.bp20.backend.api.review.domain.ReviewAnalysis;
import com.bp20.backend.api.review.dto.AspectScoreDto;
import com.bp20.backend.api.review.dto.request.ReviewAnalysisRequestDto;
import com.bp20.backend.api.review.dto.AspectSentimentDto;
import com.bp20.backend.api.review.dto.response.AspectRadarResponseDto;
import com.bp20.backend.api.review.dto.response.AspectStatResponseDto;
import com.bp20.backend.api.review.dto.response.ReviewAnalysisResponseDto;

import java.time.LocalDateTime;
import java.util.ArrayList;

import com.bp20.backend.api.review.repository.ReviewAnalysisRepository;
import com.bp20.backend.api.review.repository.ReviewRepository;
import lombok.extern.slf4j.Slf4j;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Service
public class ReviewAnalysisService {

    private final ReviewAnalysisRepository reviewAnalysisRepository;
    private final ReviewRepository reviewRepository;
    private final WebClient webClient;

    public ReviewAnalysisService(
            ReviewAnalysisRepository reviewAnalysisRepository,
            ReviewRepository reviewRepository,
            @Value("${app.fastapi.base-url}") String baseUrl
    ) {
        this.reviewAnalysisRepository = reviewAnalysisRepository;
        this.reviewRepository = reviewRepository;
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
    }

    @Transactional
    public void analyzeUnanalyzedReviews(Long storeId) {
        List<Review> unanalyzedReviews = reviewRepository.findByStoreIdAndIsAnalyzedFalse(storeId);

        if (unanalyzedReviews.isEmpty())
            return;

        List<ReviewAnalysisRequestDto> requestDtos = unanalyzedReviews.stream()
                .map(review -> new ReviewAnalysisRequestDto(review.getId(), review.getContent()))
                .toList();

        analyzeAndSaveReviews(requestDtos);
    }

    @Transactional
    public void analyzeReviewsByIds(List<Long> reviewIds) {
        List<Review> reviews = reviewRepository.findAllById(reviewIds);

        if (reviews.isEmpty())
            return;

        List<ReviewAnalysisRequestDto> requestDtos = reviews.stream()
                .map(review -> new ReviewAnalysisRequestDto(review.getId(), review.getContent()))
                .toList();

        analyzeAndSaveReviews(requestDtos);
    }


    public void analyzeAndSaveReviews(List<ReviewAnalysisRequestDto> requestDtos) {
        try {
            List<ReviewAnalysisResponseDto> responses = webClient.post()
                    .uri("/api/v1/review/analyze")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestDtos)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<ReviewAnalysisResponseDto>>() {})
                    .block();

            if (responses == null || responses.isEmpty()) return;

            List<ReviewAnalysis> entitiesToSave = new ArrayList<>();

            for (ReviewAnalysisResponseDto res : responses) {
                reviewRepository.findById(res.reviewId()).ifPresent(review -> {
                    review.markAsAnalyzed();

                    for (AspectSentimentDto result : res.results()) {
                        ReviewAnalysis entity = ReviewAnalysis.builder()
                                .reviewId(review.getId())
                                .aspect(result.aspect())
                                .sentiment(result.sentiment())
                                .confidence(result.confidence())
                                .build();
                        entitiesToSave.add(entity);
                    }
                });
            }

            reviewAnalysisRepository.saveAll(entitiesToSave);
            log.info("FastAPI 분석 완료 및 DB 적재 성공 (총 {}건 저장)", entitiesToSave.size());

        } catch (Exception e) {
            log.error("FastAPI WebClient 통신 중 에러 발생: {}", e.getMessage());
            throw new RuntimeException("AI 분석 서비스 통신 실패", e);
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
