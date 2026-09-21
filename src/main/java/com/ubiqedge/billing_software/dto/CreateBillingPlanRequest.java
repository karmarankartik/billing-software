package com.ubiqedge.billing_software.dto;

import java.math.BigDecimal;
import java.util.List;

public record CreateBillingPlanRequest(
        String name,
        String code,
        String description,
        BigDecimal pricePerUnit,
        String planType,
        List<BillingSlabRequest> slabs
) {
}