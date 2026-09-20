package com.ubiqedge.billing_software.dto;

import java.util.UUID;

public record CreateWaterMeterRequest(
        String meterNumber,
        UUID billingPlanId
) {
}