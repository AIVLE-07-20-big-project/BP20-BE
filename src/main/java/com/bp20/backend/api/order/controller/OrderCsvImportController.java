package com.bp20.backend.api.order.controller;

import com.bp20.backend.api.order.service.OrderCsvImportService;
import com.bp20.backend.global.response.ApiResponse;
import com.bp20.backend.global.response.SuccessCode;
import com.bp20.backend.global.security.principal.SecurityPrincipal;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * AI 가계부 매출 집계를 위한 메뉴/주문/주문상세 CSV 업로드 API.
 * 반드시 menu-items -> orders -> order-items 순서로 올려야 한다
 * (order-items는 orders에 이미 저장된 OrderID를 참조해서 연결한다).
 */
@Tag(name = "점주 - 매출 CSV 임포트", description = "AI 가계부 매출 집계를 위한 메뉴/주문/주문상세 CSV 업로드 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/store-owner/orders/csv")
@SecurityRequirement(name = "bearerAuth")
public class OrderCsvImportController {

    private final OrderCsvImportService orderCsvImportService;

    @PostMapping(value = "/menu-items", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadMenuItems(
            @AuthenticationPrincipal SecurityPrincipal currentUser,
            @RequestPart("file") MultipartFile file
    ) {
        int count = orderCsvImportService.loadMenuItems(currentUser.id(), file);
        return ApiResponse.success(SuccessCode.SUCCESS_MENU_ITEM_CSV_UPLOAD, Map.of("count", count));
    }

    @PostMapping(value = "/orders", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadOrders(
            @AuthenticationPrincipal SecurityPrincipal currentUser,
            @RequestPart("file") MultipartFile file
    ) {
        int count = orderCsvImportService.loadOrders(currentUser.id(), file);
        return ApiResponse.success(SuccessCode.SUCCESS_ORDER_CSV_UPLOAD, Map.of("count", count));
    }

    @PostMapping(value = "/order-items", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadOrderItems(
            @AuthenticationPrincipal SecurityPrincipal currentUser,
            @RequestPart("file") MultipartFile file
    ) {
        int count = orderCsvImportService.loadOrderItems(currentUser.id(), file);
        return ApiResponse.success(SuccessCode.SUCCESS_ORDER_ITEM_CSV_UPLOAD, Map.of("count", count));
    }

    /**
     * 현재 로그인한 점주의 매출 CSV 저장 데이터 수 확인용.
     */
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Map<String, Long>>> status(
            @AuthenticationPrincipal SecurityPrincipal currentUser
    ) {
        return ApiResponse.success(
                SuccessCode.SUCCESS_ORDER_CSV_STATUS,
                orderCsvImportService.getStatus(currentUser.id())
        );
    }
}
