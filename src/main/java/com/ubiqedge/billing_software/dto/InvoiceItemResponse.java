package com.ubiqedge.billing_software.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record InvoiceItemResponse(
        UUID id,
        UUID billingPlanId,
        LocalDate segmentStart,
        LocalDate segmentEnd,
        BigDecimal openingReading,
        BigDecimal closingReading,
        BigDecimal consumption,
        BigDecimal amount,
        Instant createdAt
) {
}