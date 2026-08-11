package com.bp20.backend.api.iam.storeowner.dto.response;

import com.bp20.backend.api.store.domain.Store;
import com.bp20.backend.api.user.domain.User;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "재인증 후 일시 공개되는 점주 개인정보 원문")
public record StoreOwnerPersonalDataResponse(
        Long id,
        String email,
        String name,
        String phoneNumber,
        String businessNumber,
        Instant visibleUntil
) {
    public static StoreOwnerPersonalDataResponse from(User user, Store store, Instant visibleUntil) {
        return new StoreOwnerPersonalDataResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getPhoneNumber(),
                store == null ? null : store.getBusinessNumber(),
                visibleUntil
        );
    }
}
