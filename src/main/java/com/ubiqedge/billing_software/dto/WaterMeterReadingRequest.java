package com.ubiqedge.billing_software.dto;



import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record WaterMeterReadingRequest(
        UUID waterMeterId,
        String readingType,
        BigDecimal readingValue,
        Instant readingAt,
        String ingestionKey
) {
}

