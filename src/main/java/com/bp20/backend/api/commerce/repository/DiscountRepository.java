package com.bp20.backend.api.commerce.repository;

import com.bp20.backend.api.commerce.domain.Discount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DiscountRepository extends JpaRepository<Discount, Long> {

    List<Discount> findByStoreIdOrderByIdDesc(Long storeId);

    @Query("select d from Discount d "
            + "where d.id = :discountId and d.store.owner.id = :ownerId")
    Optional<Discount> findOwnedDiscount(
            @Param("discountId") Long discountId,
            @Param("ownerId") Long ownerId
    );
}
