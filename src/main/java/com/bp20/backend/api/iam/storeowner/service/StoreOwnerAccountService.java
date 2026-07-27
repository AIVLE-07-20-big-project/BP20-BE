package com.bp20.backend.api.iam.storeowner.service;

import com.bp20.backend.api.iam.log.domain.IamLogAction;
import com.bp20.backend.api.iam.log.service.IamLogService;
import com.bp20.backend.api.iam.storeowner.dto.response.StoreOwnerAccountResponse;
import com.bp20.backend.api.store.domain.Store;
import com.bp20.backend.api.store.repository.StoreRepository;
import com.bp20.backend.api.user.domain.User;
import com.bp20.backend.api.user.domain.UserRole;
import com.bp20.backend.api.user.repository.UserRepository;
import com.bp20.backend.global.exception.ApiException;
import com.bp20.backend.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StoreOwnerAccountService {

    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final PasswordEncoder passwordEncoder;
    private final IamLogService iamLogService;

    @Transactional(readOnly = true)
    public List<StoreOwnerAccountResponse> getStoreOwners() {
        List<User> owners = userRepository.findByRoleOrderByIdDesc(UserRole.STORE_OWNER);
        Map<Long, Store> storesByOwnerId = storeRepository.findByOwnerIdIn(
                        owners.stream().map(User::getId).toList()
                ).stream()
                .collect(Collectors.toMap(store -> store.getOwner().getId(), Function.identity()));

        return owners.stream()
                .map(owner -> StoreOwnerAccountResponse.from(owner, storesByOwnerId.get(owner.getId())))
                .toList();
    }

    @Transactional
    public StoreOwnerAccountResponse deactivate(
            Long actorUserId,
            Long storeOwnerId,
            String currentPassword,
            String sourceIp
    ) {
        requireAdminAndPassword(actorUserId, currentPassword);
        User storeOwner = requireStoreOwner(storeOwnerId);
        storeOwner.deactivate();
        iamLogService.record(actorUserId, IamLogAction.STORE_OWNER_DEACTIVATED,
                storeOwner.getId(), storeOwner.getEmail(), sourceIp);
        return toResponse(storeOwner);
    }

    @Transactional
    public StoreOwnerAccountResponse activate(
            Long actorUserId,
            Long storeOwnerId,
            String currentPassword,
            String sourceIp
    ) {
        requireAdminAndPassword(actorUserId, currentPassword);
        User storeOwner = requireStoreOwner(storeOwnerId);
        storeOwner.activate();
        iamLogService.record(actorUserId, IamLogAction.STORE_OWNER_ACTIVATED,
                storeOwner.getId(), storeOwner.getEmail(), sourceIp);
        return toResponse(storeOwner);
    }

    private User requireStoreOwner(Long storeOwnerId) {
        return userRepository.findByIdAndRole(storeOwnerId, UserRole.STORE_OWNER)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND_USER));
    }

    private void requireAdminAndPassword(Long actorUserId, String currentPassword) {
        User actor = userRepository.findByIdWithPrivateInfo(actorUserId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND_USER));
        if (actor.getRole() != UserRole.SUPER_ADMIN && actor.getRole() != UserRole.ADMIN) {
            throw new ApiException(ErrorCode.FORBIDDEN_ADMIN_REQUIRED);
        }
        if (!passwordEncoder.matches(currentPassword, actor.getPasswordHash())) {
            throw new ApiException(ErrorCode.UNAUTHORIZED_INVALID_PASSWORD);
        }
    }

    private StoreOwnerAccountResponse toResponse(User storeOwner) {
        Store store = storeRepository.findByOwnerId(storeOwner.getId()).orElse(null);
        return StoreOwnerAccountResponse.from(storeOwner, store);
    }
}
