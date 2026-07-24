package com.bp20.backend.api.commerce.service;

import com.bp20.backend.api.commerce.domain.BundleStatus;
import com.bp20.backend.api.commerce.domain.ProductBundle;
import com.bp20.backend.api.commerce.dto.request.BundleItemRequest;
import com.bp20.backend.api.commerce.dto.request.BundleStatusRequest;
import com.bp20.backend.api.commerce.dto.request.CreateProductBundleRequest;
import com.bp20.backend.api.commerce.dto.request.UpdateProductBundleRequest;
import com.bp20.backend.api.commerce.dto.response.ProductBundleResponse;
import com.bp20.backend.api.commerce.repository.ProductBundleRepository;
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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProductBundleService {

    private final StoreRepository storeRepository;
    private final ProductRepository productRepository;
    private final ProductBundleRepository productBundleRepository;

    @Transactional
    public ProductBundleResponse create(Long ownerId, CreateProductBundleRequest request) {
        Store store = requireOwnedStore(ownerId);
        List<ProductBundle.BundleItemSpec> itemSpecs = resolveItemSpecs(
                store,
                request.items(),
                request.bundlePrice()
        );

        ProductBundle bundle = ProductBundle.create(
                store,
                request.name().trim(),
                trimToNull(request.description()),
                request.bundlePrice(),
                trimToNull(request.imageUrl())
        );
        bundle.replaceItems(itemSpecs);
        return ProductBundleResponse.from(productBundleRepository.save(bundle));
    }

    @Transactional(readOnly = true)
    public List<ProductBundleResponse> getMine(Long ownerId) {
        Store store = requireOwnedStore(ownerId);
        return productBundleRepository.findAllByStoreId(store.getId()).stream()
                .map(ProductBundleResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductBundleResponse getOne(Long ownerId, Long bundleId) {
        return ProductBundleResponse.from(requireOwnedBundle(ownerId, bundleId));
    }

    @Transactional
    public ProductBundleResponse update(
            Long ownerId,
            Long bundleId,
            UpdateProductBundleRequest request
    ) {
        ProductBundle bundle = requireOwnedBundle(ownerId, bundleId);
        List<ProductBundle.BundleItemSpec> itemSpecs = resolveItemSpecs(
                bundle.getStore(),
                request.items(),
                request.bundlePrice()
        );
        bundle.update(
                request.name().trim(),
                trimToNull(request.description()),
                request.bundlePrice(),
                trimToNull(request.imageUrl())
        );
        bundle.replaceItems(itemSpecs);
        if (bundle.getStatus() == BundleStatus.ON_SALE && !canSell(bundle)) {
            bundle.changeStatus(BundleStatus.HIDDEN);
        }
        return ProductBundleResponse.from(bundle);
    }

    @Transactional
    public ProductBundleResponse changeStatus(
            Long ownerId,
            Long bundleId,
            BundleStatusRequest request
    ) {
        ProductBundle bundle = requireOwnedBundle(ownerId, bundleId);
        if (request.status() == BundleStatus.ON_SALE && !canSell(bundle)) {
            throw new ApiException(ErrorCode.BAD_REQUEST_INVALID_BUNDLE);
        }
        bundle.changeStatus(request.status());
        return ProductBundleResponse.from(bundle);
    }

    private List<ProductBundle.BundleItemSpec> resolveItemSpecs(
            Store store,
            List<BundleItemRequest> itemRequests,
            long bundlePrice
    ) {
        Map<Long, Integer> quantities = new LinkedHashMap<>();
        for (BundleItemRequest item : itemRequests) {
            if (quantities.putIfAbsent(item.productId(), item.quantity()) != null) {
                throw new ApiException(ErrorCode.BAD_REQUEST_INVALID_BUNDLE);
            }
        }
        if (quantities.size() < 2) {
            throw new ApiException(ErrorCode.BAD_REQUEST_INVALID_BUNDLE);
        }

        List<Product> products = productRepository.findAllInStore(
                store.getId(),
                quantities.keySet()
        );
        if (products.size() != quantities.size()) {
            throw new ApiException(ErrorCode.NOT_FOUND_PRODUCT);
        }

        Map<Long, Product> productById = products.stream()
                .collect(java.util.stream.Collectors.toMap(Product::getId, product -> product));
        List<ProductBundle.BundleItemSpec> itemSpecs = quantities.entrySet().stream()
                .map(entry -> new ProductBundle.BundleItemSpec(
                        productById.get(entry.getKey()),
                        entry.getValue()
                ))
                .toList();

        long regularPrice = itemSpecs.stream()
                .mapToLong(item -> item.product().getPrice() * item.quantity())
                .sum();
        if (bundlePrice >= regularPrice) {
            throw new ApiException(ErrorCode.BAD_REQUEST_INVALID_BUNDLE);
        }
        return itemSpecs;
    }

    private boolean canSell(ProductBundle bundle) {
        return bundle.getItems().stream().allMatch(item ->
                item.getProduct().getStatus() == ProductStatus.ACTIVE
                        && item.getProduct().getStockQuantity() >= item.getQuantity()
        );
    }

    private Store requireOwnedStore(Long ownerId) {
        return storeRepository.findByOwnerId(ownerId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND_STORE));
    }

    private ProductBundle requireOwnedBundle(Long ownerId, Long bundleId) {
        return productBundleRepository.findOwnedBundle(bundleId, ownerId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND_PRODUCT_BUNDLE));
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
