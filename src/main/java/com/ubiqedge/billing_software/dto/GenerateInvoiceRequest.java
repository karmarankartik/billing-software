package com.ubiqedge.billing_software.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record GenerateInvoiceRequest(

        @NotNull
        LocalDate billingPeriodStart,

        @NotNull
        LocalDate billingPeriodEnd

) {
}