package com.bp20.backend.api.location.dto;

public record LocationSearchResponse(
        String displayName,
        double latitude,
        double longitude
) {
}
