package com.ubiqedge.billing_software.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record UpdateWaterMeterRequest(
        String meterNumber,
        UUID billingPlanId,
        BigDecimal currentTotalReading
) {
}