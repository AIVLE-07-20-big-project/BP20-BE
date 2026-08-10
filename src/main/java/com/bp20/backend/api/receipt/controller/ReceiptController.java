package com.bp20.backend.api.receipt.controller;

import com.bp20.backend.api.receipt.dto.request.ReceiptCreateRequest;
import com.bp20.backend.api.receipt.dto.request.ReceiptUpdateRequest;
import com.bp20.backend.api.receipt.dto.response.OcrParseResponse;
import com.bp20.backend.api.receipt.dto.response.ReceiptResponse;
import com.bp20.backend.api.receipt.service.ReceiptService;
import com.bp20.backend.global.response.ApiResponse;
import com.bp20.backend.global.response.PageResponse;
import com.bp20.backend.global.response.SuccessCode;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.bp20.backend.global.security.principal.SecurityPrincipal;

@Tag(name = "Receipt", description = "영수증 OCR/저장/조회 API")
@RestController
@RequestMapping("/api/store-owner/receipts")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class ReceiptController {

    private final ReceiptService receiptService;

    /**
     * 영수증 이미지를 OCR 처리해서 미리보기 결과를 돌려준다. (아직 DB에 저장하지 않음)
     * 프론트에서 이 결과를 사용자에게 보여주고, 수정 후 POST /api/receipts 로 최종 저장을 확정한다.
     */
    @PostMapping(value = "/parse", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<OcrParseResponse>> parse(
            @AuthenticationPrincipal SecurityPrincipal currentUser,
            @RequestPart("file") MultipartFile file
    ) {
        OcrParseResponse result = receiptService.parse(currentUser.id(), file);
        return ApiResponse.success(SuccessCode.SUCCESS_RECEIPT_PARSE, result);
    }

    /**
     * 사용자가 검토/수정한 영수증 데이터를 최종 저장한다.
     * 중복 의심 시 409(CONFLICT_DUPLICATE_RECEIPT)를 반환하고, force=true로 재요청하면 강제 저장한다.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ReceiptResponse>> create(
            @Valid @RequestBody ReceiptCreateRequest request
    ) {
        ReceiptResponse result = receiptService.createReceipt(request);
        return ApiResponse.success(SuccessCode.SUCCESS_RECEIPT_CREATE, result);
    }

    @GetMapping("/{receiptId}")
    public ResponseEntity<ApiResponse<ReceiptResponse>> get(@PathVariable Long receiptId) {
        ReceiptResponse result = receiptService.getReceipt(receiptId);
        return ApiResponse.success(SuccessCode.SUCCESS_RECEIPT_GET, result);
    }

    /**
     * 업로드 내역 목록. page는 0부터 시작하며, size 기본값은 30건이다.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ReceiptResponse>>> list(
            @RequestParam Long storeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size
    ) {
        PageResponse<ReceiptResponse> result = receiptService.listReceipts(storeId, page, size);
        return ApiResponse.success(SuccessCode.SUCCESS_RECEIPT_GET, result);
    }

    /**
     * 업로드 내역에서 점주가 직접 수정한다.
     */
    @PutMapping("/{receiptId}")
    public ResponseEntity<ApiResponse<ReceiptResponse>> update(
            @PathVariable Long receiptId,
            @Valid @RequestBody ReceiptUpdateRequest request
    ) {
        ReceiptResponse result = receiptService.updateReceipt(receiptId, request);
        return ApiResponse.success(SuccessCode.SUCCESS_RECEIPT_UPDATE, result);
    }

    @DeleteMapping("/{receiptId}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long receiptId) {
        receiptService.deleteReceipt(receiptId);
        return ApiResponse.successOnly(SuccessCode.SUCCESS_RECEIPT_DELETE);
    }
}
