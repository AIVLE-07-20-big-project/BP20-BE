package com.bp20.backend.api.iam.storeowner.dto.response;

import com.bp20.backend.api.store.domain.Store;
import com.bp20.backend.api.user.domain.User;
import com.bp20.backend.api.user.domain.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "점주 계정 응답")
public record StoreOwnerAccountResponse(
        Long id,
        String email,
        String name,
        String phoneNumber,
        UserStatus status,
        Long storeId,
        String storeName,
        String businessNumber,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static StoreOwnerAccountResponse from(User user, Store store) {
        return new StoreOwnerAccountResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getPhoneNumber(),
                user.getStatus(),
                store == null ? null : store.getId(),
                store == null ? null : store.getName(),
                store == null ? null : store.getBusinessNumber(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
