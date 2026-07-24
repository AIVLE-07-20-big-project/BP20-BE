package com.bp20.backend.api.product.service;

import com.bp20.backend.api.product.domain.Product;
import com.bp20.backend.api.product.domain.ProductStatus;
import com.bp20.backend.api.product.dto.request.CreateProductRequest;
import com.bp20.backend.api.product.dto.request.ProductStatusRequest;
import com.bp20.backend.api.product.dto.request.UpdateProductRequest;
import com.bp20.backend.api.product.dto.response.ProductResponse;
import com.bp20.backend.api.product.repository.ProductRepository;
import com.bp20.backend.api.store.domain.Store;
import com.bp20.backend.api.store.repository.StoreRepository;
import com.bp20.backend.global.exception.ApiException;
import com.bp20.backend.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final StoreRepository storeRepository;
    private final ProductRepository productRepository;

    @Transactional
    public ProductResponse create(Long ownerId, CreateProductRequest request) {
        Store store = requireOwnedStore(ownerId);

        Product product = productRepository.save(Product.create(
                store,
                request.name().trim(),
                trimToNull(request.description()),
                request.price(),
                request.stockQuantity(),
                trimToNull(request.imageUrl())
        ));
        return ProductResponse.from(product);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getMine(Long ownerId) {
        Store store = requireOwnedStore(ownerId);
        return productRepository.findByStoreIdOrderByIdDesc(store.getId()).stream()
                .map(ProductResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductResponse getOne(Long ownerId, Long productId) {
        return ProductResponse.from(requireOwnedProduct(ownerId, productId));
    }

    @Transactional
    public ProductResponse update(Long ownerId, Long productId, UpdateProductRequest request) {
        Product product = requireOwnedProduct(ownerId, productId);

        product.update(
                request.name().trim(),
                trimToNull(request.description()),
                request.price(),
                request.stockQuantity(),
                trimToNull(request.imageUrl())
        );
        return ProductResponse.from(product);
    }

    @Transactional
    public ProductResponse changeStatus(Long ownerId, Long productId, ProductStatusRequest request) {
        Product product = requireOwnedProduct(ownerId, productId);
        if (request.status() == ProductStatus.ACTIVE && product.getStockQuantity() == 0) {
            throw new ApiException(ErrorCode.BAD_REQUEST_INVALID_PRODUCT_STATUS);
        }
        if (request.status() == ProductStatus.SOLD_OUT && product.getStockQuantity() > 0) {
            throw new ApiException(ErrorCode.BAD_REQUEST_INVALID_PRODUCT_STATUS);
        }
        product.changeStatus(request.status());
        return ProductResponse.from(product);
    }

    private Store requireOwnedStore(Long ownerId) {
        return storeRepository.findByOwnerId(ownerId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND_STORE));
    }

    private Product requireOwnedProduct(Long ownerId, Long productId) {
        return productRepository.findOwnedProduct(productId, ownerId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND_PRODUCT));
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
