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
public class SalesMetrics {

    @JsonProperty("target_sales")
    private Double targetSales;

    @JsonProperty("visit_count")
    private Integer visitCount;

    @JsonProperty("average_order_value")
    private Double averageOrderValue;

    @JsonProperty("revisit_rate")
    private Double revisitRate;

    @JsonProperty("coupon_usage_rate")
    private Double couponUsageRate;

    @JsonProperty("new_customer_count")
    private Integer newCustomerCount;

    @JsonProperty("dormant_customer_return_count")
    private Integer dormantCustomerReturnCount;

    @JsonProperty("total_sales")
    private Double totalSales;
}