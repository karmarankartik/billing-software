package com.ubiqedge.billing_software.dto;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String username,
        String role,
        Instant createdAt,
        Instant updatedAt
) {
}