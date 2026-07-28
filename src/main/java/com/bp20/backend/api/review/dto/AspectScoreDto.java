package com.bp20.backend.api.review.dto;

import lombok.Getter;

@Getter
public class AspectScoreDto {
    private String aspect;
    private Double score;

    public AspectScoreDto(String aspect, Double score) {
        this.aspect = aspect;
        this.score = (score != null) ? score : 0.0;
    }
}
