package com.ubiqedge.billing_software.dto;
import com.ubiqedge.billing_software.constant.ReadingType;
import java.math.BigDecimal;
import java.time.Instant;

public record CreateWaterMeterReadingRequest( ReadingType readingType, BigDecimal readingValue, Instant readingAt ) { }