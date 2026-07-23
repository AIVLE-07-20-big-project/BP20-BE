package com.bp20.backend.product.service;

import com.bp20.backend.product.dto.ProductCreateRequest;
import com.bp20.backend.product.dto.ProductResponse;
import com.bp20.backend.product.dto.ProductUpdateRequest;
import com.bp20.backend.product.entity.Product;
import com.bp20.backend.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional
    public ProductResponse createProduct(ProductCreateRequest request) {
        validateDuplicateProductName(request.productName());

        Product product = new Product(
                request.productName(),
                request.category(),
                request.unit(),
                request.purchasePrice(),
                request.sellingPrice(),
                request.safetyStock()
        );

        Product savedProduct = productRepository.save(product);

        return ProductResponse.from(savedProduct);
    }

    public List<ProductResponse> getProducts() {
        return productRepository.findAllByOrderByProductIdDesc()
                .stream()
                .map(ProductResponse::from)
                .toList();
    }

    public ProductResponse getProduct(Long productId) {
        Product product = findProduct(productId);

        return ProductResponse.from(product);
    }

    @Transactional
    public ProductResponse updateProduct(
            Long productId,
            ProductUpdateRequest request
    ) {
        Product product = findProduct(productId);

        validateDuplicateProductNameForUpdate(
                request.productName(),
                productId
        );

        product.update(
                request.productName(),
                request.category(),
                request.unit(),
                request.purchasePrice(),
                request.sellingPrice(),
                request.safetyStock()
        );

        return ProductResponse.from(product);
    }

    @Transactional
    public void deleteProduct(Long productId) {
        Product product = findProduct(productId);

        productRepository.delete(product);
    }

    private Product findProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "상품을 찾을 수 없습니다. productId=" + productId
                        )
                );
    }

    private void validateDuplicateProductName(String productName) {
        if (productRepository.existsByProductName(productName)) {
            throw new IllegalArgumentException(
                    "이미 등록된 상품명입니다. productName=" + productName
            );
        }
    }

    private void validateDuplicateProductNameForUpdate(
            String productName,
            Long productId
    ) {
        boolean duplicated =
                productRepository.existsByProductNameAndProductIdNot(
                        productName,
                        productId
                );

        if (duplicated) {
            throw new IllegalArgumentException(
                    "이미 등록된 상품명입니다. productName=" + productName
            );
        }
    }
}