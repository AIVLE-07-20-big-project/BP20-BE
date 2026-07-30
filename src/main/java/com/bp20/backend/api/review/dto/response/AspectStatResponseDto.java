package com.bp20.backend.api.review.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AspectStatResponseDto {
    private String aspect;
    private Long positive;
    private Long neutral;
    private Long negative;
}
