package com.ubiqedge.billing_software.dto;

import com.ubiqedge.billing_software.entity.Invoice;
import com.ubiqedge.billing_software.entity.InvoiceItem;

import java.util.List;

public record UserInvoiceResponse(
        Invoice invoice,
        List<InvoiceItem> items
) {
}