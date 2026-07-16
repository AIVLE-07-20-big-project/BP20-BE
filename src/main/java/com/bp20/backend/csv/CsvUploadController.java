package com.bp20.backend.csv;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

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
            @RequestPart("file") MultipartFile file
    ) {
        csvDataService.loadProducts(file);

        return Map.of(
                "message", "상품 CSV 업로드 완료",
                "count", csvDataService.getProducts().size()
        );
    }

    @PostMapping(
            value = "/sales",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public Map<String, Object> uploadSales(
            @RequestPart("file") MultipartFile file
    ) {
        csvDataService.loadSales(file);

        return Map.of(
                "message", "매출 CSV 업로드 완료",
                "count", csvDataService.getSales().size()
        );
    }

    @PostMapping(
            value = "/inventories",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public Map<String, Object> uploadInventories(
            @RequestPart("file") MultipartFile file
    ) {
        csvDataService.loadInventories(file);

        return Map.of(
                "message", "재고 CSV 업로드 완료",
                "count", csvDataService.getInventories().size()
        );
    }

    /**
     * 현재 메모리에 저장된 데이터 수 확인용
     */
    @GetMapping("/status")
    public Map<String, Object> status() {
        return Map.of(
                "productCount",
                csvDataService.getProducts().size(),

                "salesCount",
                csvDataService.getSales().size(),

                "inventoryCount",
                csvDataService.getInventories().size()
        );
    }
}