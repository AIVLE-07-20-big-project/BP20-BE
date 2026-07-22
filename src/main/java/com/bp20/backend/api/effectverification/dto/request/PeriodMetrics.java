package com.bp20.backend.api.effectverification.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PeriodMetrics {

    private SalesMetrics sales;

    private ReviewMetrics review;
}