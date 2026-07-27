package com.bp20.backend.location.dto;

public record LocationSearchResponse(
        String displayName,
        double latitude,
        double longitude
) {
}
