package com.ubiqedge.billing_software.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "water_meter_assignments")
public class WaterMeterAssignment {

    @Id
    private UUID id;

    @Column(name = "water_meter_id", nullable = false)
    private UUID waterMeterId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt;

    @Column(name = "unassigned_at")
    private Instant unassignedAt;

    @Column(name = "active_assignment_key")
    private UUID activeAssignmentKey;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public WaterMeterAssignment() {
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

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public Instant getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(Instant assignedAt) {
        this.assignedAt = assignedAt;
    }

    public Instant getUnassignedAt() {
        return unassignedAt;
    }

    public void setUnassignedAt(Instant unassignedAt) {
        this.unassignedAt = unassignedAt;
    }

    public UUID getActiveAssignmentKey() {
        return activeAssignmentKey;
    }

    public void setActiveAssignmentKey(UUID activeAssignmentKey) {
        this.activeAssignmentKey = activeAssignmentKey;
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