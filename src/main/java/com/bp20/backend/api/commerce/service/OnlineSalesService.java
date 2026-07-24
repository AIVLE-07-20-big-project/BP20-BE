package com.bp20.backend.api.commerce.service;

import com.bp20.backend.api.commerce.dto.request.OnlineSalesStatusRequest;
import com.bp20.backend.api.product.domain.OnlineSalesStatus;
import com.bp20.backend.api.product.domain.Product;
import com.bp20.backend.api.product.domain.ProductStatus;
import com.bp20.backend.api.product.dto.response.ProductResponse;
import com.bp20.backend.api.product.repository.ProductRepository;
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

    @Transactional
    public StoreResponse changeStatus(Long ownerId, OnlineSalesStatusRequest request) {
        Store store = requireOwnedStore(ownerId);
        if (request.status() == com.bp20.backend.api.store.domain.OnlineSalesStatus.OPEN && !hasOnlineSaleItem(store.getId())) {
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
                        OnlineSalesStatus.NOT_REGISTERED
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
    public ProductResponse unregisterProduct(Long ownerId, Long productId) {
        Product product = requireOwnedProduct(ownerId, productId);
        if (!product.isRegisteredOnline()) {
            throw new ApiException(ErrorCode.BAD_REQUEST_INVALID_ONLINE_PRODUCT_STATUS);
        }
        product.unregisterOnline();
        return ProductResponse.from(product);
    }

    private boolean hasOnlineSaleItem(Long storeId) {
        return productRepository.existsByStoreIdAndOnlineSalesStatusAndStatusAndStockQuantityGreaterThan(
                storeId,
                OnlineSalesStatus.ON_SALE,
                ProductStatus.ACTIVE,
                0
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
}
