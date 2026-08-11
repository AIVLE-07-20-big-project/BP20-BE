package com.bp20.backend.api.commerce.order.repository;

import com.bp20.backend.api.commerce.order.domain.OnlinePurchase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OnlinePurchaseRepository extends JpaRepository<OnlinePurchase, Long> {

    @Query("select distinct purchase from OnlinePurchase purchase "
            + "join fetch purchase.customer customer "
            + "join fetch customer.privateInfo "
            + "left join fetch purchase.items item "
            + "left join fetch item.product "
            + "where purchase.store.owner.id = :ownerId "
            + "order by purchase.purchasedAt desc, purchase.id desc")
    List<OnlinePurchase> findAllOwnedBy(@Param("ownerId") Long ownerId);

    @Query("select distinct purchase from OnlinePurchase purchase "
            + "join fetch purchase.customer customer "
            + "join fetch customer.privateInfo "
            + "left join fetch purchase.items item "
            + "left join fetch item.product "
            + "where purchase.id = :purchaseId and purchase.store.owner.id = :ownerId")
    Optional<OnlinePurchase> findOwnedPurchase(
            @Param("purchaseId") Long purchaseId,
            @Param("ownerId") Long ownerId
    );
}
