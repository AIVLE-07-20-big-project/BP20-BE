package com.bp20.backend.api.store.controller;

import com.bp20.backend.api.store.dto.response.StoreReviewKeywordResponseDto;
import com.bp20.backend.api.store.dto.response.InnerStoreResponseDto;
import com.bp20.backend.api.store.service.StoreReviewKeywordService;
import com.bp20.backend.api.store.service.StoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/internal/stores")
@RequiredArgsConstructor
public class InnerStoreController {

    private final StoreReviewKeywordService storeReviewKeywordService;
    private final StoreService storeService;

    @GetMapping("/{storeId}/keywords")
    public ResponseEntity<List<StoreReviewKeywordResponseDto>> getStoreKeywords(
            @PathVariable Long storeId
    ) {
        List<StoreReviewKeywordResponseDto> response = storeReviewKeywordService.getStoreKeywords(storeId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{storeId}/context")
    public ResponseEntity<InnerStoreResponseDto> getStoreContext(
            @PathVariable Long storeId
    ) {
        return ResponseEntity.ok(storeService.getStoreContext(storeId));
    }
}
