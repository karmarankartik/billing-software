package com.ubiqedge.billing_software.entity;

import jakarta.persistence.*;
import org.hibernate.generator.EventType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "billing_generation_jobs")
public class BillingGenerationJob {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "billing_period_start", nullable = false)
    private LocalDate billingPeriodStart;

    @Column(name = "billing_period_end", nullable = false)
    private LocalDate billingPeriodEnd;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "meters_processed", nullable = false)
    private int metersProcessed;

    @Column(name = "invoices_generated", nullable = false)
    private int invoicesGenerated;

    @Column(name = "meters_skipped", nullable = false)
    private int metersSkipped;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public BillingGenerationJob() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public LocalDate getBillingPeriodStart() {
        return billingPeriodStart;
    }

    public void setBillingPeriodStart(LocalDate billingPeriodStart) {
        this.billingPeriodStart = billingPeriodStart;
    }

    public LocalDate getBillingPeriodEnd() {
        return billingPeriodEnd;
    }

    public void setBillingPeriodEnd(LocalDate billingPeriodEnd) {
        this.billingPeriodEnd = billingPeriodEnd;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getMetersProcessed() {
        return metersProcessed;
    }

    public void setMetersProcessed(int metersProcessed) {
        this.metersProcessed = metersProcessed;
    }

    public int getInvoicesGenerated() {
        return invoicesGenerated;
    }

    public void setInvoicesGenerated(int invoicesGenerated) {
        this.invoicesGenerated = invoicesGenerated;
    }

    public int getMetersSkipped() {
        return metersSkipped;
    }

    public void setMetersSkipped(int metersSkipped) {
        this.metersSkipped = metersSkipped;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }


    // getters and setters
}