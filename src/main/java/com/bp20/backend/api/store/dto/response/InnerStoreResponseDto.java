package com.bp20.backend.api.store.dto.response;

import com.bp20.backend.api.store.domain.Store;

public record InnerStoreResponseDto(
        String name,
        String category,
        String address
) {
    public static InnerStoreResponseDto from(Store store) {
        return new InnerStoreResponseDto(
                store.getName(),
                store.getCategory(),
                store.getAddress()
        );
    }
}
