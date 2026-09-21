package com.ubiqedge.billing_software.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record InvoiceResponse(
        UUID id,
        UUID userId,
        UUID waterMeterId,
        UUID assignmentId,
        LocalDate billingPeriodStart,
        LocalDate billingPeriodEnd,
        BigDecimal totalConsumption,
        BigDecimal totalAmount,
        Instant generatedAt,
        Instant createdAt,
        List<InvoiceItemResponse> items
) {
}