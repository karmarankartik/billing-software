package com.ubiqedge.billing_software.dto;

import java.util.UUID;

public record WaterMeterAssignmentRequest(UUID waterMeterId, UUID userId ) { }