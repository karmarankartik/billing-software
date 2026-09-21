package com.ubiqedge.billing_software.dto;

import com.ubiqedge.billing_software.entity.Invoice;

import java.util.List;

public record UserInvoiceGenerationResponse(
        List<Invoice> invoices,
        int totalMeters,
        int metersProcessed,
        int invoicesGenerated,
        int metersSkipped,
        List<SkippedMeterResponse> skippedMeters
) {
}