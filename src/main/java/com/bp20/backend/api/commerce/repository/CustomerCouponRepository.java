package com.bp20.backend.api.commerce.repository;

import com.bp20.backend.api.commerce.domain.CustomerCoupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CustomerCouponRepository extends JpaRepository<CustomerCoupon, Long> {

    boolean existsByCode(String code);

    @Query("select c from CustomerCoupon c "
            + "join fetch c.store s "
            + "join fetch c.customer "
            + "where s.owner.id = :ownerId order by c.id desc")
    List<CustomerCoupon> findAllOwnedBy(@Param("ownerId") Long ownerId);

    @Query("select c from CustomerCoupon c "
            + "join fetch c.store s "
            + "join fetch c.customer "
            + "where c.id = :couponId and s.owner.id = :ownerId")
    Optional<CustomerCoupon> findOwnedCoupon(
            @Param("couponId") Long couponId,
            @Param("ownerId") Long ownerId
    );
}
