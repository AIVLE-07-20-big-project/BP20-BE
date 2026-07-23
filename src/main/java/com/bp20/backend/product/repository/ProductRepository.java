package com.bp20.backend.product.repository;

import com.bp20.backend.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    boolean existsByProductName(String productName);

    boolean existsByProductNameAndProductIdNot(
            String productName,
            Long productId
    );

    List<Product> findAllByOrderByProductIdDesc();
}