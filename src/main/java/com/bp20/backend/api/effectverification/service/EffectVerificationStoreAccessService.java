package com.bp20.backend.api.effectverification.service;

import com.bp20.backend.api.store.domain.Store;
import com.bp20.backend.api.store.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class EffectVerificationStoreAccessService {

    private final StoreRepository storeRepository;

    @Value("${effect-verification.mock-public-access:false}")
    private boolean mockPublicAccess;

    @Transactional(readOnly = true)
    public void validateOwner(Long userId, Long storeId) {
        if (mockPublicAccess) {
            return;
        }
        if (userId == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authentication is required"
            );
        }

        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Store not found"
                ));
        if (!userId.equals(store.getOwner().getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You do not have access to this store"
            );
        }
    }

    @Transactional(readOnly = true)
    public Long resolveOwnedStoreId(Long userId, Long requestedStoreId) {
        if (userId == null) {
            if (mockPublicAccess) {
                return requestedStoreId;
            }
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authentication is required"
            );
        }

        return storeRepository.findByOwnerId(userId)
                .map(Store::getId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Store not found for authenticated owner"
                ));
    }
}
