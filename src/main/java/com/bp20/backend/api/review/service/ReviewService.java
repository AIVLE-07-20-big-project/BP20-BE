package com.bp20.backend.api.review.service;

import com.bp20.backend.api.review.dto.response.ReviewResponseDto;
import com.bp20.backend.api.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    public final ReviewRepository reviewRepository;

    @Transactional(readOnly = true)
    public List<ReviewResponseDto> getReviewsByStoreId(Long storeId) {
        return reviewRepository.findByStoreId(storeId).stream()
                .map(ReviewResponseDto::from)
                .toList();
    }
}
