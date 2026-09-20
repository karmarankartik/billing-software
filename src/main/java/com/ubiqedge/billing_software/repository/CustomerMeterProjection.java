package com.ubiqedge.billing_software.repository;



import java.time.Instant;
import java.util.UUID;

public interface CustomerMeterProjection {

    UUID getId();

    String getMeterNumber();

    UUID getBillingPlanId();

    Instant getCreatedAt();

    Instant getUpdatedAt();
}

