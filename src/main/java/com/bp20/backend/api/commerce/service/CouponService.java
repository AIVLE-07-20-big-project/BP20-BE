package com.bp20.backend.api.commerce.service;

import com.bp20.backend.api.commerce.domain.Coupon;
import com.bp20.backend.api.commerce.domain.CouponStatus;
import com.bp20.backend.api.commerce.domain.DiscountType;
import com.bp20.backend.api.commerce.dto.request.IssueCouponRequest;
import com.bp20.backend.api.commerce.dto.response.CouponResponse;
import com.bp20.backend.api.commerce.repository.CouponRepository;
import com.bp20.backend.api.customer.domain.Customer;
import com.bp20.backend.api.customer.repository.CustomerRepository;
import com.bp20.backend.api.store.domain.Store;
import com.bp20.backend.api.store.repository.StoreRepository;
import com.bp20.backend.global.exception.ApiException;
import com.bp20.backend.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final StoreRepository storeRepository;
    private final CustomerRepository customerRepository;
    private final CouponRepository couponRepository;

    @Transactional
    public CouponResponse issue(Long ownerId, IssueCouponRequest request) {
        Store store = storeRepository.findByOwnerId(ownerId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND_STORE));
        LocalDateTime now = LocalDateTime.now();
        if (!request.expiresAt().isAfter(now)
                || (request.discountType() == DiscountType.RATE && request.discountValue() > 100)) {
            throw new ApiException(ErrorCode.BAD_REQUEST_INVALID_COUPON);
        }

        Customer customer = customerRepository.findOwnedCustomer(request.customerId(), ownerId)
                .filter(Customer::isActive)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND_CUSTOMER));
        Coupon coupon = Coupon.issue(
                store,
                customer,
                request.name().trim(),
                request.discountType(),
                request.discountValue(),
                now,
                request.expiresAt()
        );
        return CouponResponse.from(couponRepository.save(coupon));
    }

    @Transactional
    public List<CouponResponse> getMine(Long ownerId) {
        LocalDateTime now = LocalDateTime.now();
        return couponRepository.findAllOwnedBy(ownerId).stream()
                .peek(coupon -> expireIfNecessary(coupon, now))
                .map(CouponResponse::from)
                .toList();
    }

    @Transactional
    public CouponResponse getOne(Long ownerId, Long couponId) {
        Coupon coupon = requireOwnedCoupon(ownerId, couponId);
        expireIfNecessary(coupon, LocalDateTime.now());
        return CouponResponse.from(coupon);
    }

    @Transactional
    public CouponResponse revoke(Long ownerId, Long couponId) {
        Coupon coupon = requireOwnedCoupon(ownerId, couponId);
        LocalDateTime now = LocalDateTime.now();
        expireIfNecessary(coupon, now);
        if (coupon.getStatus() != CouponStatus.ISSUED) {
            throw new ApiException(ErrorCode.BAD_REQUEST_INVALID_COUPON);
        }
        coupon.revoke(now);
        return CouponResponse.from(coupon);
    }

    private void expireIfNecessary(Coupon coupon, LocalDateTime now) {
        if (coupon.getStatus() == CouponStatus.ISSUED && !now.isBefore(coupon.getExpiresAt())) {
            coupon.expire();
        }
    }

    private Coupon requireOwnedCoupon(Long ownerId, Long couponId) {
        return couponRepository.findOwnedCoupon(couponId, ownerId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND_COUPON));
    }
}
