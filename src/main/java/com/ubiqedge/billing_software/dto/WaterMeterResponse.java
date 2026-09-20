package com.ubiqedge.billing_software.dto;

import java.time.Instant;
import java.util.UUID;

public record WaterMeterResponse(
        UUID id,
        String meterNumber,
        UUID billingPlanId,
        Instant createdAt,
        Instant updatedAt
) {
}