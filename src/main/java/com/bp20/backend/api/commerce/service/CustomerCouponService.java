package com.bp20.backend.api.commerce.service;

import com.bp20.backend.api.commerce.domain.CouponStatus;
import com.bp20.backend.api.commerce.domain.CustomerCoupon;
import com.bp20.backend.api.commerce.domain.DiscountType;
import com.bp20.backend.api.commerce.dto.request.IssueCouponRequest;
import com.bp20.backend.api.commerce.dto.response.CustomerCouponResponse;
import com.bp20.backend.api.commerce.repository.CustomerCouponRepository;
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
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomerCouponService {

    private final StoreRepository storeRepository;
    private final CustomerRepository customerRepository;
    private final CustomerCouponRepository customerCouponRepository;

    @Transactional
    public CustomerCouponResponse issue(Long ownerId, IssueCouponRequest request) {
        Store store = storeRepository.findByOwnerId(ownerId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND_STORE));
        LocalDateTime now = LocalDateTime.now();
        if (!request.expiresAt().isAfter(now)
                || (request.discountType() == DiscountType.RATE && request.discountValue() > 100)) {
            throw new ApiException(ErrorCode.BAD_REQUEST_INVALID_COUPON);
        }

        Customer customer = customerRepository.findById(request.customerId())
                .filter(Customer::isActive)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND_CUSTOMER));
        CustomerCoupon coupon = CustomerCoupon.issue(
                store,
                customer,
                request.name().trim(),
                request.discountType(),
                request.discountValue(),
                generateUniqueCode(),
                now,
                request.expiresAt()
        );
        return CustomerCouponResponse.from(customerCouponRepository.save(coupon));
    }

    @Transactional
    public List<CustomerCouponResponse> getMine(Long ownerId) {
        LocalDateTime now = LocalDateTime.now();
        return customerCouponRepository.findAllOwnedBy(ownerId).stream()
                .peek(coupon -> expireIfNecessary(coupon, now))
                .map(CustomerCouponResponse::from)
                .toList();
    }

    @Transactional
    public CustomerCouponResponse getOne(Long ownerId, Long couponId) {
        CustomerCoupon coupon = requireOwnedCoupon(ownerId, couponId);
        expireIfNecessary(coupon, LocalDateTime.now());
        return CustomerCouponResponse.from(coupon);
    }

    @Transactional
    public CustomerCouponResponse revoke(Long ownerId, Long couponId) {
        CustomerCoupon coupon = requireOwnedCoupon(ownerId, couponId);
        LocalDateTime now = LocalDateTime.now();
        expireIfNecessary(coupon, now);
        if (coupon.getStatus() != CouponStatus.ISSUED) {
            throw new ApiException(ErrorCode.BAD_REQUEST_INVALID_COUPON);
        }
        coupon.revoke(now);
        return CustomerCouponResponse.from(coupon);
    }

    private void expireIfNecessary(CustomerCoupon coupon, LocalDateTime now) {
        if (coupon.getStatus() == CouponStatus.ISSUED && !now.isBefore(coupon.getExpiresAt())) {
            coupon.expire();
        }
    }

    private String generateUniqueCode() {
        String code;
        do {
            code = UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase();
        } while (customerCouponRepository.existsByCode(code));
        return code;
    }

    private CustomerCoupon requireOwnedCoupon(Long ownerId, Long couponId) {
        return customerCouponRepository.findOwnedCoupon(couponId, ownerId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND_CUSTOMER_COUPON));
    }
}
