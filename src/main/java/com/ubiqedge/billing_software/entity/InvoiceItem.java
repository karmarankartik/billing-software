package com.ubiqedge.billing_software.entity;

import jakarta.persistence.*;
import org.hibernate.generator.EventType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "invoice_items")
public class InvoiceItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "invoice_id", nullable = false)
    private UUID invoiceId;

    @Column(name = "billing_plan_id", nullable = false)
    private UUID billingPlanId;

    @Column(name = "segment_start", nullable = false)
    private Instant segmentStart;

    @Column(name = "segment_end", nullable = false)
    private Instant segmentEnd;

    @Column(name = "opening_reading", nullable = false, precision = 19, scale = 6)
    private BigDecimal openingReading;

    @Column(name = "closing_reading", nullable = false, precision = 19, scale = 6)
    private BigDecimal closingReading;

    @Column(name = "consumption", nullable = false, precision = 19, scale = 6)
    private BigDecimal consumption;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public InvoiceItem() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(UUID invoiceId) {
        this.invoiceId = invoiceId;
    }

    public Instant getSegmentStart() {
        return segmentStart;
    }

    public void setSegmentStart(Instant segmentStart) {
        this.segmentStart = segmentStart;
    }

    public UUID getBillingPlanId() {
        return billingPlanId;
    }

    public void setBillingPlanId(UUID billingPlanId) {
        this.billingPlanId = billingPlanId;
    }

    public BigDecimal getOpeningReading() {
        return openingReading;
    }

    public void setOpeningReading(BigDecimal openingReading) {
        this.openingReading = openingReading;
    }

    public Instant getSegmentEnd() {
        return segmentEnd;
    }

    public void setSegmentEnd(Instant segmentEnd) {
        this.segmentEnd = segmentEnd;
    }

    public BigDecimal getClosingReading() {
        return closingReading;
    }

    public void setClosingReading(BigDecimal closingReading) {
        this.closingReading = closingReading;
    }

    public BigDecimal getConsumption() {
        return consumption;
    }

    public void setConsumption(BigDecimal consumption) {
        this.consumption = consumption;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
// getters and setters
}