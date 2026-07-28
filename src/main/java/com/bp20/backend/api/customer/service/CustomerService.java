package com.bp20.backend.api.customer.service;

import com.bp20.backend.api.customer.domain.Customer;
import com.bp20.backend.api.customer.dto.request.CreateCustomerRequest;
import com.bp20.backend.api.customer.dto.response.CustomerResponse;
import com.bp20.backend.api.customer.repository.CustomerRepository;
import com.bp20.backend.api.store.domain.Store;
import com.bp20.backend.api.store.repository.StoreRepository;
import com.bp20.backend.api.user.domain.UserPrivateInfo;
import com.bp20.backend.api.user.repository.UserPrivateInfoRepository;
import com.bp20.backend.global.exception.ApiException;
import com.bp20.backend.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final StoreRepository storeRepository;
    private final CustomerRepository customerRepository;
    private final UserPrivateInfoRepository userPrivateInfoRepository;

    @Transactional
    public CustomerResponse create(Long ownerId, CreateCustomerRequest request) {
        Store store = requireOwnedStore(ownerId);
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (customerRepository.existsByStoreIdAndPrivateInfoEmailIgnoreCase(store.getId(), email)) {
            throw new ApiException(ErrorCode.CONFLICT_DUPLICATE_CUSTOMER_EMAIL);
        }

        UserPrivateInfo privateInfo = userPrivateInfoRepository.findByEmailIgnoreCase(email)
                .map(this::requireCustomerOnlyPrivateInfo)
                .orElseGet(() -> userPrivateInfoRepository.save(UserPrivateInfo.forCustomer(
                        email,
                        request.name().trim(),
                        trimToNull(request.phoneNumber())
                )));

        Customer customer = Customer.create(store, privateInfo);
        return CustomerResponse.from(customerRepository.save(customer));
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> getMine(Long ownerId) {
        Store store = requireOwnedStore(ownerId);
        return customerRepository.findAllInStore(store.getId()).stream()
                .map(CustomerResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public CustomerResponse getOne(Long ownerId, Long customerId) {
        return CustomerResponse.from(requireOwnedCustomer(ownerId, customerId));
    }

    private Store requireOwnedStore(Long ownerId) {
        return storeRepository.findByOwnerId(ownerId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND_STORE));
    }

    private UserPrivateInfo requireCustomerOnlyPrivateInfo(UserPrivateInfo privateInfo) {
        if (privateInfo.hasPassword()) {
            throw new ApiException(ErrorCode.CONFLICT_DUPLICATE_EMAIL);
        }
        return privateInfo;
    }

    private Customer requireOwnedCustomer(Long ownerId, Long customerId) {
        return customerRepository.findOwnedCustomer(customerId, ownerId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND_CUSTOMER));
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
