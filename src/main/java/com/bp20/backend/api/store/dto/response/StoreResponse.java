package com.bp20.backend.api.store.dto.response;

import com.bp20.backend.api.store.domain.Store;
import com.bp20.backend.api.store.domain.OnlineSalesStatus;

import java.time.LocalDateTime;

public record StoreResponse(
        Long id,
        String name,
        String businessNumber,
        String category,
        String address,
        String phoneNumber,
        OnlineSalesStatus onlineSalesStatus,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static StoreResponse from(Store store) {
        return new StoreResponse(
                store.getId(),
                store.getName(),
                store.getBusinessNumber(),
                store.getCategory(),
                store.getAddress(),
                store.getPhoneNumber(),
                store.getOnlineSalesStatus(),
                store.getCreatedAt(),
                store.getUpdatedAt()
        );
    }
}
