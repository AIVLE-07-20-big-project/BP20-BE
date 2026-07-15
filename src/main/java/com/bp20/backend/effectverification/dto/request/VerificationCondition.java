package com.bp20.backend.effectverification.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VerificationCondition {

    @JsonProperty("period_days")
    private Integer periodDays;

    @JsonProperty("start_hour")
    private Integer startHour;

    @JsonProperty("end_hour")
    private Integer endHour;

    @JsonProperty("compare_same_weekday")
    private Boolean compareSameWeekday;

    @JsonProperty("target_aspect")
    private String targetAspect;
}