package com.bp20.backend.effectverification.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class MetricResult {

    @JsonProperty("metric_name")
    private String metricName;

    @JsonProperty("before_value")
    private Double beforeValue;

    @JsonProperty("after_value")
    private Double afterValue;

    @JsonProperty("change_value")
    private Double changeValue;

    @JsonProperty("change_rate")
    private Double changeRate;

    private Boolean improved;
}