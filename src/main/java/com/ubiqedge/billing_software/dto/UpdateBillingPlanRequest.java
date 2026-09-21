package com.ubiqedge.billing_software.dto;

import java.math.BigDecimal;
import java.util.List;

public record UpdateBillingPlanRequest(String name, String description, BigDecimal pricePerUnit, String planType, List<BillingSlabRequest> slabs ) { }