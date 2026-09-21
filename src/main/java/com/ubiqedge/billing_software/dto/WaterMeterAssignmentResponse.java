package com.ubiqedge.billing_software.dto;


import java.time.Instant;
import java.util.UUID;

public record WaterMeterAssignmentResponse(
        UUID id,
        UUID waterMeterId,
        UUID userId,
        Instant assignedAt,
        Instant unassignedAt,
        UUID createdBy,
        Instant createdAt
) {
}