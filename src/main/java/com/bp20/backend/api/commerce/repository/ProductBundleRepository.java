package com.bp20.backend.api.commerce.repository;

import com.bp20.backend.api.commerce.domain.ProductBundle;
import com.bp20.backend.api.product.domain.OnlineSalesItemStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProductBundleRepository extends JpaRepository<ProductBundle, Long> {

    boolean existsByStoreIdAndOnlineSalesStatus(Long storeId, OnlineSalesItemStatus onlineSalesStatus);

    @Query("select distinct b from ProductBundle b "
            + "left join fetch b.items i "
            + "left join fetch i.product "
            + "where b.store.id = :storeId order by b.id desc")
    List<ProductBundle> findAllByStoreId(@Param("storeId") Long storeId);

    @Query("select distinct b from ProductBundle b "
            + "left join fetch b.items i "
            + "left join fetch i.product "
            + "where b.id = :bundleId and b.store.owner.id = :ownerId")
    Optional<ProductBundle> findOwnedBundle(
            @Param("bundleId") Long bundleId,
            @Param("ownerId") Long ownerId
    );

    @Query("select b from ProductBundle b "
            + "where b.store.id = :storeId and b.id in :bundleIds")
    List<ProductBundle> findAllInStore(
            @Param("storeId") Long storeId,
            @Param("bundleIds") Collection<Long> bundleIds
    );
}
