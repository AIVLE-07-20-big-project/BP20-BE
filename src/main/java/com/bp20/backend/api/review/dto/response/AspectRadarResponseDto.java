package com.bp20.backend.api.review.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AspectRadarResponseDto {
    private String aspect;
    private Double currentScore;
    private Double prevScore;
}
