package com.bp20.backend.api.product.repository;

import com.bp20.backend.api.product.domain.OnlineSalesStatus;
import com.bp20.backend.api.product.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByStoreIdOrderByIdDesc(Long storeId);

    /**
     * "내 상품"(온라인 판매 관리) 목록용 - CSV로 임포트된 메뉴 행(sourceProductId != null)은
     * 판매 상품 목록에 섞이면 안 되므로 제외한다.
     */
    List<Product> findByStoreIdAndSourceProductIdIsNullOrderByIdDesc(Long storeId);

    List<Product> findByStoreIdAndOnlineSalesStatusNotOrderByIdDesc(
            Long storeId,
            OnlineSalesStatus onlineSalesStatus
    );

    @Query("select p from Product p where p.id = :productId and p.store.owner.id = :ownerId")
    Optional<Product> findOwnedProduct(
            @Param("productId") Long productId,
            @Param("ownerId") Long ownerId
    );

    Optional<Product> findByStoreIdAndSourceProductId(Long storeId, Long sourceProductId);

    long countByStoreIdAndSourceProductIdIsNotNull(Long storeId);

}
