package com.ubiqedge.billing_software.dto;

import java.util.UUID;

public record SkippedMeterResponse(
        UUID waterMeterId,
        String reason
) {
}