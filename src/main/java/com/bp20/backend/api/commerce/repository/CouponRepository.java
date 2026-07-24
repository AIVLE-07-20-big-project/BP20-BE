package com.bp20.backend.api.commerce.repository;

import com.bp20.backend.api.commerce.domain.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

    @Query("select c from Coupon c "
            + "join fetch c.store s "
            + "join fetch c.customer customer "
            + "join fetch customer.privateInfo "
            + "where s.owner.id = :ownerId order by c.id desc")
    List<Coupon> findAllOwnedBy(@Param("ownerId") Long ownerId);

    @Query("select c from Coupon c "
            + "join fetch c.store s "
            + "join fetch c.customer customer "
            + "join fetch customer.privateInfo "
            + "where c.id = :couponId and s.owner.id = :ownerId")
    Optional<Coupon> findOwnedCoupon(
            @Param("couponId") Long couponId,
            @Param("ownerId") Long ownerId
    );
}
