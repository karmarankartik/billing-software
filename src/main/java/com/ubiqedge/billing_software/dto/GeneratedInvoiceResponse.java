package com.ubiqedge.billing_software.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record GeneratedInvoiceResponse(
        UUID invoiceId,
        UUID waterMeterId,
        UUID userId,
        UUID assignmentId,
        LocalDate billingPeriodStart,
        LocalDate billingPeriodEnd,
        Instant segmentStart,
        Instant segmentEnd,
        BigDecimal openingReading,
        BigDecimal closingReading,
        BigDecimal consumption,
        UUID billingPlanId,
        String planType,
        BigDecimal amount
) {
}