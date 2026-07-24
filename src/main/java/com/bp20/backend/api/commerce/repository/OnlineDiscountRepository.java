package com.bp20.backend.api.commerce.repository;

import com.bp20.backend.api.commerce.domain.OnlineDiscount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OnlineDiscountRepository extends JpaRepository<OnlineDiscount, Long> {

    List<OnlineDiscount> findByStoreIdOrderByIdDesc(Long storeId);

    @Query("select d from OnlineDiscount d "
            + "where d.id = :discountId and d.store.owner.id = :ownerId")
    Optional<OnlineDiscount> findOwnedDiscount(
            @Param("discountId") Long discountId,
            @Param("ownerId") Long ownerId
    );
}
