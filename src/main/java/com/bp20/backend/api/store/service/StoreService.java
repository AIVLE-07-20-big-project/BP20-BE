package com.bp20.backend.api.store.service;

import com.bp20.backend.api.store.domain.Store;
import com.bp20.backend.api.store.dto.request.CreateStoreRequest;
import com.bp20.backend.api.store.dto.request.UpdateStoreRequest;
import com.bp20.backend.api.store.dto.response.StoreResponse;
import com.bp20.backend.api.store.repository.StoreRepository;
import com.bp20.backend.api.user.domain.User;
import com.bp20.backend.api.user.repository.UserRepository;
import com.bp20.backend.global.exception.ApiException;
import com.bp20.backend.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StoreService {

    private final UserRepository userRepository;
    private final StoreRepository storeRepository;

    @Transactional
    public StoreResponse create(Long ownerId, CreateStoreRequest request) {
        User owner = requireStoreOwner(ownerId);
        if (storeRepository.existsByOwnerId(ownerId)) {
            throw new ApiException(ErrorCode.CONFLICT_STORE_ALREADY_EXISTS);
        }

        String businessNumber = normalizeBusinessNumber(request.businessNumber());
        if (storeRepository.existsByBusinessNumber(businessNumber)) {
            throw new ApiException(ErrorCode.CONFLICT_DUPLICATE_BUSINESS_NUMBER);
        }

        Store store = storeRepository.save(Store.create(
                owner,
                request.name().trim(),
                businessNumber,
                request.category().trim(),
                request.address().trim(),
                trimToNull(request.phoneNumber())
        ));
        return StoreResponse.from(store);
    }

    @Transactional(readOnly = true)
    public StoreResponse getMine(Long ownerId) {
        return StoreResponse.from(requireOwnedStore(ownerId));
    }

    @Transactional
    public StoreResponse updateMine(Long ownerId, UpdateStoreRequest request) {
        Store store = requireOwnedStore(ownerId);
        store.update(
                request.name().trim(),
                request.category().trim(),
                request.address().trim(),
                trimToNull(request.phoneNumber())
        );
        return StoreResponse.from(store);
    }

    private User requireStoreOwner(Long ownerId) {
        User user = userRepository.findById(ownerId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND_USER));
        if (!user.isStoreOwner()) {
            throw new ApiException(ErrorCode.FORBIDDEN_STORE_OWNER_REQUIRED);
        }
        return user;
    }

    private Store requireOwnedStore(Long ownerId) {
        return storeRepository.findByOwnerId(ownerId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND_STORE));
    }

    private String normalizeBusinessNumber(String businessNumber) {
        return businessNumber.replace("-", "");
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
