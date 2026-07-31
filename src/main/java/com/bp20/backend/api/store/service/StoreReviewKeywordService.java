package com.bp20.backend.api.store.service;

import com.bp20.backend.api.store.dto.response.StoreReviewKeywordResponseDto;
import com.bp20.backend.api.store.repository.StoreReviewKeywordRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class StoreReviewKeyword {
    private static StoreReviewKeywordRepository storeReviewKeywordRepository;

    public List<StoreReviewKeywordResponseDto> getStoreKeywords(Long storeId) {
        return storeReviewKeywordRepository.findByStoreIdOrderByCountDesc(storeId)
                .stream()
                .map(k -> new StoreReviewKeywordResponseDto(
                        k.getAspect(),
                        k.getSentiment(),
                        k.getKeyword(),
                        k.getCount(),
                        k.getMatchedReviewIds(),
                        k.getOriginalExpressions()
                ))
                .toList();
    }

}
