package com.bp20.backend.api.commerce.repository;

import com.bp20.backend.api.commerce.domain.Discount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
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

    /**
     * AI 가계부 리포트(원가율 계산)에서 상품별 "현재 적용 중인 할인율"을 조회할 때 쓴다
     * (PR #35 리뷰 코멘트 반영 - 예전 MenuItem.discountRate 대체). RATE 타입에 한해서만 %로
     * 쓸 수 있어 FIXED_AMOUNT는 제외한다.
     */
    @Query("select d from Discount d "
            + "where d.store.id = :storeId and d.discountType = com.bp20.backend.api.commerce.domain.DiscountType.RATE "
            + "and d.status = com.bp20.backend.api.commerce.domain.DiscountStatus.ACTIVE "
            + "and d.startsAt <= :now and d.endsAt >= :now")
    List<Discount> findActiveRateDiscountsByStore(
            @Param("storeId") Long storeId,
            @Param("now") LocalDateTime now
    );
}
