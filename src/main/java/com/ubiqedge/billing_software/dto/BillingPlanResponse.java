package com.ubiqedge.billing_software.dto;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
public record BillingPlanResponse( UUID id, String name, String code, String description, BigDecimal pricePerUnit, boolean active, Instant createdAt, Instant updatedAt )
{}