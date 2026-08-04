package com.bp20.backend.api.receipt.service;

import com.bp20.backend.api.receipt.client.OcrServiceClient;
import com.bp20.backend.api.receipt.domain.Receipt;
import com.bp20.backend.api.receipt.domain.ReceiptItem;
import com.bp20.backend.api.receipt.domain.ReceiptStatus;
import com.bp20.backend.api.receipt.dto.request.ReceiptCreateRequest;
import com.bp20.backend.api.receipt.dto.request.ReceiptUpdateRequest;
import com.bp20.backend.api.receipt.dto.response.OcrParseResponse;
import com.bp20.backend.api.receipt.dto.response.ReceiptItemData;
import com.bp20.backend.api.receipt.dto.response.ReceiptResponse;
import com.bp20.backend.api.receipt.repository.ReceiptRepository;
import com.bp20.backend.api.product.repository.ProductRepository;
import com.bp20.backend.api.store.domain.Store;
import com.bp20.backend.api.store.repository.StoreRepository;
import com.bp20.backend.api.user.domain.User;
import com.bp20.backend.api.user.repository.UserRepository;
import com.bp20.backend.global.exception.ApiException;
import com.bp20.backend.global.response.ErrorCode;
import com.bp20.backend.global.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReceiptService {

    private final ReceiptRepository receiptRepository;
    private final OcrServiceClient ocrServiceClient;
    private final StoreRepository storeRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    /**
     * 영수증 이미지를 OCR 처리한다. (DB 저장은 하지 않음 - 프론트에서 검토/수정 후 createReceipt 호출)
     */
    public OcrParseResponse parse(Long ownerId, MultipartFile file) {
        List<OcrServiceClient.ProductCatalogPayload> catalog = storeRepository.findByOwnerId(ownerId)
                .map(store -> productRepository.findByStoreIdOrderByIdDesc(store.getId()).stream()
                        .map(product -> new OcrServiceClient.ProductCatalogPayload(
                                product.getName(), product.getPrice()))
                        .toList())
                .orElseGet(List::of);
        return ocrServiceClient.parseReceipt(file, catalog);
    }

    @Transactional
    public ReceiptResponse createReceipt(ReceiptCreateRequest request) {
        String dedupeKey = buildDedupeKey(
                request.storeName(), request.transactionDate(), request.transactionTime(), request.totalAmount());

        ReceiptStatus status = ReceiptStatus.CONFIRMED;
        String finalDedupeKey = dedupeKey;
        Store store = request.storeId() == null
                ? null
                : storeRepository.findById(request.storeId())
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND_STORE));
        User uploadedBy = request.uploadedByUserId() == null
                ? null
                : userRepository.findById(request.uploadedByUserId())
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND_USER));

        boolean duplicate = receiptRepository.existsByDedupeKey(dedupeKey);
        if (duplicate) {
            if (!request.force()) {
                throw new ApiException(ErrorCode.CONFLICT_DUPLICATE_RECEIPT);
            }
            status = ReceiptStatus.DUPLICATE_SUSPECTED;
            finalDedupeKey = makeUniqueDedupeKey(dedupeKey);
        }

        Receipt receipt = Receipt.create(
                store,
                uploadedBy,
                request.documentType(),
                request.storeName(),
                request.businessNumber(),
                parseDate(request.transactionDate()),
                parseTime(request.transactionTime()),
                request.paymentMethod(),
                request.category(),
                request.supplyAmount(),
                request.vat(),
                request.taxFreeAmount(),
                request.totalAmount(),
                finalDedupeKey,
                request.rawImagePath(),
                status
        );

        List<ReceiptItemData> items = request.items() != null ? request.items() : List.of();
        int lineNumber = 1;
        for (ReceiptItemData item : items) {
            receipt.addItem(ReceiptItem.create(
                    lineNumber++, item.itemName(), item.quantity(), item.unit(),
                    item.unitPrice(), item.totalPrice()
            ));
        }

        Receipt saved = receiptRepository.save(receipt);
        return ReceiptResponse.from(saved);
    }

    public ReceiptResponse getReceipt(Long receiptId) {
        Receipt receipt = receiptRepository.findById(receiptId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND_RECEIPT));
        return ReceiptResponse.from(receipt);
    }

    @Transactional
    public ReceiptResponse updateReceipt(Long receiptId, ReceiptUpdateRequest request) {
        Receipt receipt = receiptRepository.findById(receiptId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND_RECEIPT));
        String dedupeKey = buildDedupeKey(
                request.storeName(), request.transactionDate(), request.transactionTime(), request.totalAmount());
        receiptRepository.findByDedupeKey(dedupeKey)
                .filter(existing -> !existing.getId().equals(receiptId))
                .ifPresent(existing -> {
                    throw new ApiException(ErrorCode.CONFLICT_DUPLICATE_RECEIPT);
                });

        receipt.update(request.documentType(), request.storeName(), request.businessNumber(),
                parseDate(request.transactionDate()), parseTime(request.transactionTime()),
                request.paymentMethod(), request.category(), request.supplyAmount(), request.vat(),
                request.taxFreeAmount(), request.totalAmount(), dedupeKey);

        List<ReceiptItemData> itemData = request.items() != null ? request.items() : List.of();
        List<ReceiptItem> items = new java.util.ArrayList<>();
        int lineNumber = 1;
        for (ReceiptItemData item : itemData) {
            items.add(ReceiptItem.create(lineNumber++, item.itemName(), item.quantity(), item.unit(),
                    item.unitPrice(), item.totalPrice()));
        }
        receipt.replaceItems(items);
        return ReceiptResponse.from(receipt);
    }

    public List<ReceiptResponse> listReceipts(Long storeId) {
        return receiptRepository.findByStore_IdOrderByTransactionDateDesc(storeId).stream()
                .map(ReceiptResponse::from)
                .toList();
    }

    public PageResponse<ReceiptResponse> listReceipts(Long storeId, int page, int size) {
        Page<Receipt> result = receiptRepository.findByStoreIdOrderByTransactionDateDesc(
                storeId, PageRequest.of(page, size));
        return PageResponse.from(result.map(ReceiptResponse::from));
    }

    /**
     * 업로드 내역에서 점주가 직접 수정한다. 날짜/상호명/총액이 바뀌면 dedupeKey도 다시 계산해서
     * 다른 영수증과 중복되지 않는지 확인한다 (자기 자신과의 비교는 제외).
     */
    @Transactional
    public ReceiptResponse updateReceipt(Long receiptId, ReceiptUpdateRequest request) {
        Receipt receipt = receiptRepository.findById(receiptId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND_RECEIPT));

        String dedupeKey = buildDedupeKey(
                request.storeName(), request.transactionDate(), request.transactionTime(), request.totalAmount());

        if (!dedupeKey.equals(receipt.getDedupeKey())) {
            Optional<Receipt> existing = receiptRepository.findByDedupeKey(dedupeKey);
            if (existing.isPresent() && !existing.get().getId().equals(receiptId)) {
                throw new ApiException(ErrorCode.CONFLICT_DUPLICATE_RECEIPT);
            }
        }

        List<ReceiptItemData> itemData = request.items() != null ? request.items() : List.of();
        List<ReceiptItem> newItems = new ArrayList<>();
        int lineNumber = 1;
        for (ReceiptItemData item : itemData) {
            newItems.add(ReceiptItem.create(
                    lineNumber++, item.itemName(), item.quantity(), item.unit(),
                    item.unitPrice(), item.totalPrice()
            ));
        }

        receipt.update(
                request.documentType(),
                request.storeName(),
                request.businessNumber(),
                parseDate(request.transactionDate()),
                parseTime(request.transactionTime()),
                request.paymentMethod(),
                request.category(),
                request.supplyAmount(),
                request.vat(),
                request.taxFreeAmount(),
                request.totalAmount(),
                dedupeKey,
                newItems
        );

        return ReceiptResponse.from(receipt);
    }

    @Transactional
    public void deleteReceipt(Long receiptId) {
        Receipt receipt = receiptRepository.findById(receiptId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND_RECEIPT));
        receiptRepository.delete(receipt);
    }

    /**
     * Python 쪽 build_dedupe_key()와 동일한 규칙으로 생성한다.
     * (상호명 공백제거)_(날짜)_(시각)_(총액)
     */
    private String buildDedupeKey(String vendorName, String transactionDate, String transactionTime, Integer totalAmount) {
        String normalizedVendor = vendorName == null ? "" : vendorName.replace(" ", "");
        String time = transactionTime == null ? "" : transactionTime;
        long total = totalAmount == null ? 0 : totalAmount;
        return normalizedVendor + "_" + transactionDate + "_" + time + "_" + total;
    }

    /**
     * 이미 존재하는 dedupeKey에 "#2", "#3" 접미사를 붙여 중복 확정 저장 시에도 UNIQUE 제약과 충돌하지 않게 한다.
     */
    private String makeUniqueDedupeKey(String baseKey) {
        long count = receiptRepository.count(); // 정확한 접두사 카운트가 필요하면 커스텀 쿼리로 교체 권장
        String candidate = baseKey;
        int suffix = 2;
        while (receiptRepository.existsByDedupeKey(candidate)) {
            candidate = baseKey + "#" + suffix;
            suffix++;
        }
        return candidate;
    }

    private LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw new ApiException(ErrorCode.BAD_REQUEST_INVALID_INPUT, e);
        }
    }

    private LocalTime parseTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalTime.parse(value);
        } catch (DateTimeParseException e) {
            throw new ApiException(ErrorCode.BAD_REQUEST_INVALID_INPUT, e);
        }
    }
}
