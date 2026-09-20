package com.ubiqedge.billing_software.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

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

    @Column(name = "opening_reading", nullable = false, precision = 19, scale = 6)
    private BigDecimal openingReading;

    @Column(name = "closing_reading", nullable = false, precision = 19, scale = 6)
    private BigDecimal closingReading;

    @Column(name = "consumption", nullable = false, precision = 19, scale = 6)
    private BigDecimal consumption;

    @Column(name = "billing_plan_id", nullable = false)
    private UUID billingPlanId;

    @Column(name = "billing_plan_slab_id")
    private UUID billingPlanSlabId;

    @Column(name = "slab_lower_bound", precision = 19, scale = 6)
    private BigDecimal slabLowerBound;

    @Column(name = "slab_upper_bound", precision = 19, scale = 6)
    private BigDecimal slabUpperBound;

    @Column(name = "price_per_unit", nullable = false, precision = 19, scale = 4)
    private BigDecimal pricePerUnit;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalAmount;

    @Column(name = "period_from", nullable = false)
    private Instant periodFrom;

    @Column(name = "period_to", nullable = false)
    private Instant periodTo;

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

    public BigDecimal getOpeningReading() {
        return openingReading;
    }

    public void setOpeningReading(BigDecimal openingReading) {
        this.openingReading = openingReading;
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

    public UUID getBillingPlanId() {
        return billingPlanId;
    }

    public void setBillingPlanId(UUID billingPlanId) {
        this.billingPlanId = billingPlanId;
    }

    public UUID getBillingPlanSlabId() {
        return billingPlanSlabId;
    }

    public void setBillingPlanSlabId(UUID billingPlanSlabId) {
        this.billingPlanSlabId = billingPlanSlabId;
    }

    public BigDecimal getSlabLowerBound() {
        return slabLowerBound;
    }

    public void setSlabLowerBound(BigDecimal slabLowerBound) {
        this.slabLowerBound = slabLowerBound;
    }

    public BigDecimal getSlabUpperBound() {
        return slabUpperBound;
    }

    public void setSlabUpperBound(BigDecimal slabUpperBound) {
        this.slabUpperBound = slabUpperBound;
    }

    public BigDecimal getPricePerUnit() {
        return pricePerUnit;
    }

    public void setPricePerUnit(BigDecimal pricePerUnit) {
        this.pricePerUnit = pricePerUnit;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Instant getPeriodFrom() {
        return periodFrom;
    }

    public void setPeriodFrom(Instant periodFrom) {
        this.periodFrom = periodFrom;
    }

    public Instant getPeriodTo() {
        return periodTo;
    }

    public void setPeriodTo(Instant periodTo) {
        this.periodTo = periodTo;
    }
}