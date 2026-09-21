package com.ubiqedge.billing_software.dto;

import java.math.BigDecimal;

public record BillingSlabRequest(
        BigDecimal lowerBound,
        BigDecimal upperBound,
        BigDecimal pricePerUnit
) {
}