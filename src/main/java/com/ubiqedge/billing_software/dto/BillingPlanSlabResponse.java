package com.ubiqedge.billing_software.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record BillingPlanSlabResponse(UUID id, BigDecimal lowerBound, BigDecimal upperBound, BigDecimal pricePerUnit, Instant createdAt, Instant updatedAt ) { }