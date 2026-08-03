package com.bp20.backend.api.store.controller;

import com.bp20.backend.api.store.dto.response.StoreReviewKeywordResponseDto;
import com.bp20.backend.api.store.service.StoreReviewKeywordService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v3/stores/{storeId}")
@RequiredArgsConstructor
public class StoreReviewKeywordController {

    private final StoreReviewKeywordService storeReviewKeywordService;

    @GetMapping("/reviews/keywords")
    public ResponseEntity<List<StoreReviewKeywordResponseDto>> getStoreReviewKeywords(
            @PathVariable Long storeId
    ) {
        List<StoreReviewKeywordResponseDto> response = storeReviewKeywordService.getStoreKeywords(storeId);
        return ResponseEntity.ok(response);
    }
}
