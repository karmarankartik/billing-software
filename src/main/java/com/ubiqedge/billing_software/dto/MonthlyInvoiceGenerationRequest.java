package com.ubiqedge.billing_software.dto;

public record MonthlyInvoiceGenerationRequest(
        Integer year,
        Integer month
) {
}