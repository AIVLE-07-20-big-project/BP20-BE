package com.bp20.backend.api.csv.controller;

import com.bp20.backend.api.csv.service.CsvDataService;
import lombok.RequiredArgsConstructor;
import com.bp20.backend.global.security.principal.SecurityPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestPart;

import java.util.Map;
import java.util.List;
import com.bp20.backend.api.recommendation.dto.InventoryDataRequest;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/csv")
public class CsvUploadController {

    private final CsvDataService csvDataService;

    @PostMapping(
            value = "/products",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public Map<String, Object> uploadProducts(
            @AuthenticationPrincipal SecurityPrincipal currentUser,
            @RequestPart("file") MultipartFile file
    ) {
        int count = csvDataService.loadProducts(currentUser.id(), file);

        return Map.of(
                "message", "상품 CSV 업로드 완료",
                "count", count
        );
    }

    @PostMapping(
            value = "/sales",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public Map<String, Object> uploadSales(
            @AuthenticationPrincipal SecurityPrincipal currentUser,
            @RequestPart("file") MultipartFile file
    ) {
        int count = csvDataService.loadSales(currentUser.id(), file);

        return Map.of(
                "message", "매출 CSV 업로드 완료",
                "count", count
        );
    }

    @PostMapping(
            value = "/inventories",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public Map<String, Object> uploadInventories(
            @AuthenticationPrincipal SecurityPrincipal currentUser,
            @RequestPart("file") MultipartFile file
    ) {
        int count = csvDataService.loadInventories(currentUser.id(), file);
        return Map.of(
                "message", "재고 CSV 업로드 완료",
                "count", count
        );
    }

    @GetMapping("/inventories")
    public List<InventoryDataRequest> getInventories(
            @AuthenticationPrincipal SecurityPrincipal currentUser
    ) {
        return csvDataService.getInventories(currentUser.id());
    }

    /**
     * 현재 로그인한 점주의 MySQL 저장 데이터 수 확인용
     */
    @GetMapping("/status")
    public Map<String, Long> status(
            @AuthenticationPrincipal SecurityPrincipal currentUser
    ) {
        return csvDataService.getStatus(currentUser.id());
    }
}
