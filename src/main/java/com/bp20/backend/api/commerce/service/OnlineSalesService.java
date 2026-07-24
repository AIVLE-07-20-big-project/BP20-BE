package com.bp20.backend.api.commerce.service;

import com.bp20.backend.api.commerce.domain.BundleStatus;
import com.bp20.backend.api.commerce.domain.ProductBundle;
import com.bp20.backend.api.commerce.dto.request.OnlineSalesStatusRequest;
import com.bp20.backend.api.commerce.dto.response.ProductBundleResponse;
import com.bp20.backend.api.commerce.repository.ProductBundleRepository;
import com.bp20.backend.api.product.domain.OnlineSalesItemStatus;
import com.bp20.backend.api.product.domain.Product;
import com.bp20.backend.api.product.domain.ProductStatus;
import com.bp20.backend.api.product.dto.request.OnlineSalesItemStatusRequest;
import com.bp20.backend.api.product.dto.response.ProductResponse;
import com.bp20.backend.api.product.repository.ProductRepository;
import com.bp20.backend.api.store.domain.OnlineSalesStatus;
import com.bp20.backend.api.store.domain.Store;
import com.bp20.backend.api.store.dto.response.StoreResponse;
import com.bp20.backend.api.store.repository.StoreRepository;
import com.bp20.backend.global.exception.ApiException;
import com.bp20.backend.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OnlineSalesService {

    private final StoreRepository storeRepository;
    private final ProductRepository productRepository;
    private final ProductBundleRepository productBundleRepository;

    @Transactional
    public StoreResponse changeStatus(Long ownerId, OnlineSalesStatusRequest request) {
        Store store = storeRepository.findByOwnerId(ownerId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND_STORE));

        if (request.status() == OnlineSalesStatus.OPEN
                && !hasOnlineSaleItem(store.getId())) {
            throw new ApiException(ErrorCode.BAD_REQUEST_INVALID_STORE_STATUS);
        }

        store.changeOnlineSalesStatus(request.status());
        return StoreResponse.from(store);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getRegisteredProducts(Long ownerId) {
        Store store = requireOwnedStore(ownerId);
        return productRepository.findByStoreIdAndOnlineSalesStatusNotOrderByIdDesc(
                        store.getId(),
                        OnlineSalesItemStatus.NOT_REGISTERED
                ).stream()
                .map(ProductResponse::from)
                .toList();
    }

    @Transactional
    public ProductResponse registerProduct(Long ownerId, Long productId) {
        Product product = requireOwnedProduct(ownerId, productId);
        if (product.isRegisteredOnline()
                || product.getStatus() != ProductStatus.ACTIVE
                || product.getStockQuantity() == 0) {
            throw new ApiException(ErrorCode.BAD_REQUEST_INVALID_ONLINE_PRODUCT_STATUS);
        }
        product.registerOnline();
        return ProductResponse.from(product);
    }

    @Transactional
    public ProductResponse changeProductStatus(
            Long ownerId,
            Long productId,
            OnlineSalesItemStatusRequest request
    ) {
        Product product = requireOwnedProduct(ownerId, productId);
        validateOnlineSalesItemStatus(product, request.status());
        product.changeOnlineSalesStatus(request.status());
        return ProductResponse.from(product);
    }

    @Transactional
    public ProductResponse unregisterProduct(Long ownerId, Long productId) {
        Product product = requireOwnedProduct(ownerId, productId);
        if (!product.isRegisteredOnline()) {
            throw new ApiException(ErrorCode.BAD_REQUEST_INVALID_ONLINE_PRODUCT_STATUS);
        }
        product.unregisterOnline();
        return ProductResponse.from(product);
    }

    @Transactional
    public ProductBundleResponse registerBundle(Long ownerId, Long bundleId) {
        ProductBundle bundle = requireOwnedBundle(ownerId, bundleId);
        if (bundle.isRegisteredOnline()
                || bundle.getStatus() != BundleStatus.ON_SALE
                || !canSellBundle(bundle)) {
            throw new ApiException(ErrorCode.BAD_REQUEST_INVALID_BUNDLE);
        }
        bundle.registerOnline();
        return ProductBundleResponse.from(bundle);
    }

    @Transactional
    public ProductBundleResponse changeBundleStatus(
            Long ownerId,
            Long bundleId,
            OnlineSalesItemStatusRequest request
    ) {
        ProductBundle bundle = requireOwnedBundle(ownerId, bundleId);
        if (!bundle.isRegisteredOnline() || request.status() == OnlineSalesItemStatus.NOT_REGISTERED) {
            throw new ApiException(ErrorCode.BAD_REQUEST_INVALID_BUNDLE);
        }
        if (request.status() == OnlineSalesItemStatus.ON_SALE
                && (bundle.getStatus() != BundleStatus.ON_SALE || !canSellBundle(bundle))) {
            throw new ApiException(ErrorCode.BAD_REQUEST_INVALID_BUNDLE);
        }
        bundle.changeOnlineSalesStatus(request.status());
        return ProductBundleResponse.from(bundle);
    }

    @Transactional
    public ProductBundleResponse unregisterBundle(Long ownerId, Long bundleId) {
        ProductBundle bundle = requireOwnedBundle(ownerId, bundleId);
        if (!bundle.isRegisteredOnline()) {
            throw new ApiException(ErrorCode.BAD_REQUEST_INVALID_BUNDLE);
        }
        bundle.unregisterOnline();
        return ProductBundleResponse.from(bundle);
    }

    private boolean hasOnlineSaleItem(Long storeId) {
        return productRepository.existsByStoreIdAndOnlineSalesStatus(
                storeId,
                OnlineSalesItemStatus.ON_SALE
        ) || productBundleRepository.existsByStoreIdAndOnlineSalesStatus(
                storeId,
                OnlineSalesItemStatus.ON_SALE
        );
    }

    private void validateOnlineSalesItemStatus(Product product, OnlineSalesItemStatus targetStatus) {
        if (!product.isRegisteredOnline() || targetStatus == OnlineSalesItemStatus.NOT_REGISTERED) {
            throw new ApiException(ErrorCode.BAD_REQUEST_INVALID_ONLINE_PRODUCT_STATUS);
        }
        if (targetStatus == OnlineSalesItemStatus.ON_SALE
                && (product.getStatus() != ProductStatus.ACTIVE || product.getStockQuantity() == 0)) {
            throw new ApiException(ErrorCode.BAD_REQUEST_INVALID_ONLINE_PRODUCT_STATUS);
        }
        if (targetStatus == OnlineSalesItemStatus.SOLD_OUT && product.getStockQuantity() > 0) {
            throw new ApiException(ErrorCode.BAD_REQUEST_INVALID_ONLINE_PRODUCT_STATUS);
        }
    }

    private boolean canSellBundle(ProductBundle bundle) {
        return bundle.getItems().stream().allMatch(item ->
                item.getProduct().getStatus() == ProductStatus.ACTIVE
                        && item.getProduct().getStockQuantity() >= item.getQuantity()
        );
    }

    private Product requireOwnedProduct(Long ownerId, Long productId) {
        return productRepository.findOwnedProduct(productId, ownerId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND_PRODUCT));
    }

    private Store requireOwnedStore(Long ownerId) {
        return storeRepository.findByOwnerId(ownerId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND_STORE));
    }

    private ProductBundle requireOwnedBundle(Long ownerId, Long bundleId) {
        return productBundleRepository.findOwnedBundle(bundleId, ownerId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND_PRODUCT_BUNDLE));
    }
}
