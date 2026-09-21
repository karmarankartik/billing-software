package com.ubiqedge.billing_software.dto;

import java.util.List;

public record UserInvoiceGenerationResponse(
        List<UserInvoiceResponse> invoices,
        int totalMeters,
        int metersProcessed,
        int invoicesGenerated,
        int metersSkipped,
        List<SkippedMeterResponse> skippedMeters
) {
}