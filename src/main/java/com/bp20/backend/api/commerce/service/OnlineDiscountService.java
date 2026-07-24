package com.bp20.backend.api.commerce.service;

import com.bp20.backend.api.commerce.domain.BundleStatus;
import com.bp20.backend.api.commerce.domain.DiscountStatus;
import com.bp20.backend.api.commerce.domain.DiscountType;
import com.bp20.backend.api.commerce.domain.OnlineDiscount;
import com.bp20.backend.api.commerce.domain.ProductBundle;
import com.bp20.backend.api.commerce.dto.request.CreateOnlineDiscountRequest;
import com.bp20.backend.api.commerce.dto.request.OnlineDiscountStatusRequest;
import com.bp20.backend.api.commerce.dto.response.OnlineDiscountResponse;
import com.bp20.backend.api.commerce.repository.OnlineDiscountRepository;
import com.bp20.backend.api.commerce.repository.ProductBundleRepository;
import com.bp20.backend.api.product.domain.OnlineSalesItemStatus;
import com.bp20.backend.api.product.domain.Product;
import com.bp20.backend.api.product.repository.ProductRepository;
import com.bp20.backend.api.store.domain.OnlineSalesStatus;
import com.bp20.backend.api.store.domain.Store;
import com.bp20.backend.api.store.repository.StoreRepository;
import com.bp20.backend.global.exception.ApiException;
import com.bp20.backend.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class OnlineDiscountService {

    private final StoreRepository storeRepository;
    private final ProductRepository productRepository;
    private final ProductBundleRepository productBundleRepository;
    private final OnlineDiscountRepository onlineDiscountRepository;

    @Transactional
    public OnlineDiscountResponse create(Long ownerId, CreateOnlineDiscountRequest request) {
        Store store = requireOwnedStore(ownerId);
        validateRequest(request);

        Set<Long> productIds = uniqueIds(request.productIds());
        Set<Long> bundleIds = uniqueIds(request.bundleIds());
        if (productIds.isEmpty() && bundleIds.isEmpty()) {
            throw new ApiException(ErrorCode.BAD_REQUEST_INVALID_DISCOUNT);
        }

        List<Product> products = findRegisteredProducts(store, productIds);
        List<ProductBundle> bundles = findRegisteredBundles(store, bundleIds);
        validateDiscountValue(request, products, bundles);

        OnlineDiscount discount = OnlineDiscount.create(
                store,
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
        products.forEach(discount::addProduct);
        bundles.forEach(discount::addBundle);
        return OnlineDiscountResponse.from(onlineDiscountRepository.save(discount));
    }

    @Transactional(readOnly = true)
    public List<OnlineDiscountResponse> getMine(Long ownerId) {
        Store store = requireOwnedStore(ownerId);
        return onlineDiscountRepository.findByStoreIdOrderByIdDesc(store.getId()).stream()
                .map(OnlineDiscountResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public OnlineDiscountResponse getOne(Long ownerId, Long discountId) {
        return OnlineDiscountResponse.from(requireOwnedDiscount(ownerId, discountId));
    }

    @Transactional
    public OnlineDiscountResponse changeStatus(
            Long ownerId,
            Long discountId,
            OnlineDiscountStatusRequest request
    ) {
        OnlineDiscount discount = requireOwnedDiscount(ownerId, discountId);
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
        return OnlineDiscountResponse.from(discount);
    }

    private void validateRequest(CreateOnlineDiscountRequest request) {
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

    private Set<Long> uniqueIds(List<Long> ids) {
        Set<Long> uniqueIds = new LinkedHashSet<>(ids);
        if (uniqueIds.size() != ids.size() || uniqueIds.contains(null)) {
            throw new ApiException(ErrorCode.BAD_REQUEST_INVALID_DISCOUNT);
        }
        return uniqueIds;
    }

    private List<Product> findRegisteredProducts(Store store, Set<Long> productIds) {
        if (productIds.isEmpty()) {
            return List.of();
        }
        List<Product> products = productRepository.findAllInStore(store.getId(), productIds);
        if (products.size() != productIds.size()) {
            throw new ApiException(ErrorCode.NOT_FOUND_PRODUCT);
        }
        if (products.stream().anyMatch(product -> !product.isRegisteredOnline())) {
            throw new ApiException(ErrorCode.BAD_REQUEST_INVALID_DISCOUNT);
        }
        return products;
    }

    private List<ProductBundle> findRegisteredBundles(Store store, Set<Long> bundleIds) {
        if (bundleIds.isEmpty()) {
            return List.of();
        }
        List<ProductBundle> bundles = productBundleRepository.findAllInStore(store.getId(), bundleIds);
        if (bundles.size() != bundleIds.size()) {
            throw new ApiException(ErrorCode.NOT_FOUND_PRODUCT_BUNDLE);
        }
        if (bundles.stream().anyMatch(bundle -> !bundle.isRegisteredOnline())) {
            throw new ApiException(ErrorCode.BAD_REQUEST_INVALID_DISCOUNT);
        }
        return bundles;
    }

    private void validateDiscountValue(
            CreateOnlineDiscountRequest request,
            List<Product> products,
            List<ProductBundle> bundles
    ) {
        if (request.discountType() != DiscountType.FIXED_AMOUNT) {
            return;
        }
        boolean exceedsProductPrice = products.stream()
                .anyMatch(product -> request.discountValue() > product.getPrice());
        boolean exceedsBundlePrice = bundles.stream()
                .anyMatch(bundle -> request.discountValue() > bundle.getBundlePrice());
        if (exceedsProductPrice || exceedsBundlePrice) {
            throw new ApiException(ErrorCode.BAD_REQUEST_INVALID_DISCOUNT);
        }
    }

    private void validateActivation(OnlineDiscount discount, LocalDateTime now) {
        boolean unavailableProduct = discount.getDiscountProducts().stream()
                .anyMatch(link ->
                        link.getProduct().getOnlineSalesStatus() != OnlineSalesItemStatus.ON_SALE
                );
        boolean unavailableBundle = discount.getDiscountBundles().stream()
                .anyMatch(link ->
                        link.getBundle().getOnlineSalesStatus() != OnlineSalesItemStatus.ON_SALE
                                || link.getBundle().getStatus() != BundleStatus.ON_SALE
                );
        if (discount.getStore().getOnlineSalesStatus() != OnlineSalesStatus.OPEN
                || now.isBefore(discount.getStartsAt())
                || !now.isBefore(discount.getEndsAt())
                || unavailableProduct
                || unavailableBundle) {
            throw new ApiException(ErrorCode.BAD_REQUEST_INVALID_DISCOUNT);
        }
    }

    private Store requireOwnedStore(Long ownerId) {
        return storeRepository.findByOwnerId(ownerId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND_STORE));
    }

    private OnlineDiscount requireOwnedDiscount(Long ownerId, Long discountId) {
        return onlineDiscountRepository.findOwnedDiscount(discountId, ownerId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND_ONLINE_DISCOUNT));
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
