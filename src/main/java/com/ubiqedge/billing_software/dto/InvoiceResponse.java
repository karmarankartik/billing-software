package com.ubiqedge.billing_software.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record InvoiceResponse(
        UUID id,
        UUID waterMeterId,
        UUID userId,
        Integer billingMonth,
        Integer billingYear,
        BigDecimal openingReading,
        BigDecimal closingReading,
        BigDecimal consumption,
        UUID billingPlanId,
        BigDecimal pricePerUnit,
        BigDecimal totalAmount,
        Instant generatedAt
) {
}