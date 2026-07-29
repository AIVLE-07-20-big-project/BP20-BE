package com.bp20.backend.api.location.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SaveLocationRequest(
        @NotBlank
        @Size(max = 200)
        String displayName,

        @DecimalMin("-90.0")
        @DecimalMax("90.0")
        double latitude,

        @DecimalMin("-180.0")
        @DecimalMax("180.0")
        double longitude
) {
}
