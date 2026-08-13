package com.bp20.backend.api.review.service;

import com.bp20.backend.api.review.domain.Review;
import com.bp20.backend.api.review.dto.request.TestReviewCreateRequest;
import com.bp20.backend.api.review.dto.response.ReviewResponseDto;
import com.bp20.backend.api.review.repository.ReviewRepository;
import com.bp20.backend.api.store.domain.Store;
import com.bp20.backend.api.store.repository.StoreRepository;
import com.bp20.backend.global.exception.ApiException;
import com.bp20.backend.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ReviewService {

    public final ReviewRepository reviewRepository;
    private final StoreRepository storeRepository;

    @Transactional(readOnly = true)
    public List<ReviewResponseDto> getReviewsByStoreId(Long storeId) {
        return reviewRepository.findByStore_Id(storeId).stream()
                .map(ReviewResponseDto::from)
                .toList();
    }

    @Transactional
    public ReviewResponseDto createTestReview(Long storeId, Long ownerId, TestReviewCreateRequest request) {
        Store store = storeRepository.findByIdAndOwnerId(storeId, ownerId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND_STORE));

        return ReviewResponseDto.from(reviewRepository.save(createReview(store, request)));
    }

    @Transactional
    public List<ReviewResponseDto> createTestReviews(
            Long storeId,
            Long ownerId,
            List<TestReviewCreateRequest> requests
    ) {
        Store store = storeRepository.findByIdAndOwnerId(storeId, ownerId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND_STORE));

        List<Review> reviews = requests.stream()
                .map(request -> createReview(store, request))
                .toList();

        return reviewRepository.saveAll(reviews).stream()
                .map(ReviewResponseDto::from)
                .toList();
    }

    private Review createReview(Store store, TestReviewCreateRequest request) {
        return Review.builder()
                .store(store)
                .rating(request.rating())
                .content(request.content().trim())
                .reviewedDate(request.reviewedDate() != null ? request.reviewedDate() : LocalDateTime.now())
                .isAnalyzed(false)
                .build();
    }
}
