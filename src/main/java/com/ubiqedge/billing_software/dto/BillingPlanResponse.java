package com.ubiqedge.billing_software.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record BillingPlanResponse(UUID id, String name, String code, String description, String planType, BigDecimal pricePerUnit, boolean active, UUID createdBy, Instant createdAt, UUID updatedBy, Instant updatedAt, List<BillingPlanSlabResponse> slabs ) { }