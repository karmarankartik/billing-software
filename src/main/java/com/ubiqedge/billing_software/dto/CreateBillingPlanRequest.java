package com.ubiqedge.billing_software.dto;
import java.math.BigDecimal;
public record CreateBillingPlanRequest( String name, String code, String description, BigDecimal pricePerUnit )
{}