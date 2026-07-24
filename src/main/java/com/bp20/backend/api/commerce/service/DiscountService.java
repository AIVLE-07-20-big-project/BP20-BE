package com.bp20.backend.api.commerce.service;

import com.bp20.backend.api.commerce.domain.Discount;
import com.bp20.backend.api.commerce.domain.DiscountStatus;
import com.bp20.backend.api.commerce.domain.DiscountType;
import com.bp20.backend.api.commerce.dto.request.CreateDiscountRequest;
import com.bp20.backend.api.commerce.dto.request.DiscountStatusRequest;
import com.bp20.backend.api.commerce.dto.response.DiscountResponse;
import com.bp20.backend.api.commerce.repository.DiscountRepository;
import com.bp20.backend.api.product.domain.Product;
import com.bp20.backend.api.product.domain.ProductStatus;
import com.bp20.backend.api.product.repository.ProductRepository;
import com.bp20.backend.api.store.domain.Store;
import com.bp20.backend.api.store.repository.StoreRepository;
import com.bp20.backend.global.exception.ApiException;
import com.bp20.backend.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DiscountService {

    private final StoreRepository storeRepository;
    private final ProductRepository productRepository;
    private final DiscountRepository discountRepository;

    @Transactional
    public DiscountResponse create(Long ownerId, CreateDiscountRequest request) {
        Store store = requireOwnedStore(ownerId);
        validateRequest(request);
        Product product = productRepository.findOwnedProduct(request.productId(), ownerId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND_PRODUCT));
        validateDiscountValue(request, product);

        Discount discount = Discount.create(
                store,
                product,
                request.name().trim(),
                trimToNull(request.description()),
                request.discountType(),
                request.discountValue(),
                request.startsAt(),
                request.endsAt(),
                request.dailyStartTime(),
                request.dailyEndTime(),
                request.reminderEnabled()
        );
        return DiscountResponse.from(discountRepository.save(discount));
    }

    @Transactional(readOnly = true)
    public List<DiscountResponse> getMine(Long ownerId) {
        Store store = requireOwnedStore(ownerId);
        return discountRepository.findByStoreIdOrderByIdDesc(store.getId()).stream()
                .map(DiscountResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public DiscountResponse getOne(Long ownerId, Long discountId) {
        return DiscountResponse.from(requireOwnedDiscount(ownerId, discountId));
    }

    @Transactional
    public DiscountResponse changeStatus(
            Long ownerId,
            Long discountId,
            DiscountStatusRequest request
    ) {
        Discount discount = requireOwnedDiscount(ownerId, discountId);
        DiscountStatus targetStatus = request.status();
        if (targetStatus == DiscountStatus.DRAFT) {
            throw new ApiException(ErrorCode.BAD_REQUEST_INVALID_DISCOUNT);
        }

        LocalDateTime now = LocalDateTime.now();
        if (targetStatus == DiscountStatus.SCHEDULED && !discount.getStartsAt().isAfter(now)) {
            throw new ApiException(ErrorCode.BAD_REQUEST_INVALID_DISCOUNT);
        }
        if (targetStatus == DiscountStatus.ACTIVE) {
            validateActivation(discount, now);
        }
        discount.changeStatus(targetStatus);
        return DiscountResponse.from(discount);
    }

    private void validateRequest(CreateDiscountRequest request) {
        if (!request.endsAt().isAfter(request.startsAt())) {
            throw new ApiException(ErrorCode.BAD_REQUEST_INVALID_DISCOUNT);
        }
        if ((request.dailyStartTime() == null) != (request.dailyEndTime() == null)) {
            throw new ApiException(ErrorCode.BAD_REQUEST_INVALID_DISCOUNT);
        }
        if (request.dailyStartTime() != null
                && request.dailyStartTime().equals(request.dailyEndTime())) {
            throw new ApiException(ErrorCode.BAD_REQUEST_INVALID_DISCOUNT);
        }
        if (request.discountType() == DiscountType.RATE && request.discountValue() > 100) {
            throw new ApiException(ErrorCode.BAD_REQUEST_INVALID_DISCOUNT);
        }
    }

    private void validateDiscountValue(CreateDiscountRequest request, Product product) {
        if (request.discountType() == DiscountType.FIXED_AMOUNT
                && request.discountValue() > product.getPrice()) {
            throw new ApiException(ErrorCode.BAD_REQUEST_INVALID_DISCOUNT);
        }
    }

    private void validateActivation(Discount discount, LocalDateTime now) {
        Product product = discount.getProduct();
        if (now.isBefore(discount.getStartsAt())
                || !now.isBefore(discount.getEndsAt())
                || product.getStatus() != ProductStatus.ACTIVE
                || product.getStockQuantity() == 0) {
            throw new ApiException(ErrorCode.BAD_REQUEST_INVALID_DISCOUNT);
        }
    }

    private Store requireOwnedStore(Long ownerId) {
        return storeRepository.findByOwnerId(ownerId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND_STORE));
    }

    private Discount requireOwnedDiscount(Long ownerId, Long discountId) {
        return discountRepository.findOwnedDiscount(discountId, ownerId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND_DISCOUNT));
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
