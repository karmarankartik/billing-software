package com.ubiqedge.billing_software.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "water_meter_billing_plans")
public class WaterMeterBillingPlan {

    @Id
    private UUID id;

    @Column(name = "water_meter_id", nullable = false)
    private UUID waterMeterId;

    @Column(name = "billing_plan_id", nullable = false)
    private UUID billingPlanId;

    @Column(name = "effective_from", nullable = false)
    private Instant effectiveFrom;

    @Column(name = "effective_to")
    private Instant effectiveTo;

    @Column(name = "active_plan_key")
    private UUID activePlanKey;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public WaterMeterBillingPlan() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getWaterMeterId() {
        return waterMeterId;
    }

    public void setWaterMeterId(UUID waterMeterId) {
        this.waterMeterId = waterMeterId;
    }

    public UUID getBillingPlanId() {
        return billingPlanId;
    }

    public void setBillingPlanId(UUID billingPlanId) {
        this.billingPlanId = billingPlanId;
    }

    public Instant getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setEffectiveFrom(Instant effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }

    public Instant getEffectiveTo() {
        return effectiveTo;
    }

    public void setEffectiveTo(Instant effectiveTo) {
        this.effectiveTo = effectiveTo;
    }

    public UUID getActivePlanKey() {
        return activePlanKey;
    }

    public void setActivePlanKey(UUID activePlanKey) {
        this.activePlanKey = activePlanKey;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}