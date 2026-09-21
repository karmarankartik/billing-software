package com.ubiqedge.billing_software.dto;

import java.util.UUID;

public record InvoiceGenerationResponse(
        UUID jobId,
        String status
) {
}