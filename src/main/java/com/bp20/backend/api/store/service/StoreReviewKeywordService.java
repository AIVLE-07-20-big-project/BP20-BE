package com.bp20.backend.api.store.service;

import com.bp20.backend.api.store.domain.StoreReviewKeyword;
import com.bp20.backend.api.store.dto.response.StoreReviewKeywordResponseDto;
import com.bp20.backend.api.store.repository.StoreReviewKeywordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoreReviewKeywordService {

    private final StoreReviewKeywordRepository storeReviewKeywordRepository;

    public List<StoreReviewKeywordResponseDto> getStoreKeywords(Long storeId) {
        LocalDateTime now = LocalDateTime.now();

        LocalDateTime startOfThisMonth = now.withDayOfMonth(1).with(LocalTime.MIN);
        LocalDateTime startOfLastMonth = startOfThisMonth.minusMonths(1);

        List<StoreReviewKeyword> thisMonthKeywords =
                storeReviewKeywordRepository.findByStore_IdAndAnalyzedAtGreaterThanEqualOrderByCountDesc(storeId, startOfThisMonth);

        List<StoreReviewKeyword> lastMonthKeywords =
                storeReviewKeywordRepository.findByStore_IdAndAnalyzedAtBetween(storeId, startOfLastMonth, startOfThisMonth);

        Map<String, Integer> lastMonthMap = new HashMap<>();
        if (lastMonthKeywords != null) {
            for (StoreReviewKeyword k : lastMonthKeywords) {
                if (k.getKeyword() != null) {
                    lastMonthMap.put(k.getKeyword(), k.getCount() != null ? k.getCount() : 0);
                }
            }
        }

        return thisMonthKeywords.stream().map(curr -> {
            int currentCount = curr.getCount() != null ? curr.getCount() : 0;
            int prevCount = lastMonthMap.getOrDefault(curr.getKeyword(), 0);

            Double changeRate = calculateChangeRate(currentCount, prevCount);

            return StoreReviewKeywordResponseDto.of(curr, changeRate);
        }).toList();
    }

    private Double calculateChangeRate(int current, int prev) {
        if (prev == 0) {
            return null;
        }
        double rate = ((double) (current - prev) / prev) * 100.0;
        return Math.round(rate * 10.0) / 10.0;
    }
}