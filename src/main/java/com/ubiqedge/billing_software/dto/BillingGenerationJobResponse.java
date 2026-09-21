package com.ubiqedge.billing_software.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record BillingGenerationJobResponse(
        UUID jobId,
        LocalDate billingPeriodStart,
        LocalDate billingPeriodEnd,
        String status,
        int metersProcessed,
        int invoicesGenerated,
        int metersSkipped,
        Instant startedAt,
        Instant completedAt,
        List<SkippedMeterResponse> skippedMeters
) {
}