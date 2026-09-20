package com.ubiqedge.billing_software.entity;



import com.ubiqedge.billing_software.constant.ReadingType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "water_meter_readings")
public class WaterMeterReading {

    @Id
    private UUID id;

    @Column(name = "water_meter_id", nullable = false)
    private UUID waterMeterId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reading_type", nullable = false, length = 20)
    private ReadingType readingType;

    @Column(name = "reading_value", nullable = false, precision = 19, scale = 6)
    private BigDecimal readingValue;

    @Column(name = "reading_at", nullable = false)
    private Instant readingAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public WaterMeterReading() {
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

    public ReadingType getReadingType() {
        return readingType;
    }

    public void setReadingType(ReadingType readingType) {
        this.readingType = readingType;
    }

    public BigDecimal getReadingValue() {
        return readingValue;
    }

    public void setReadingValue(BigDecimal readingValue) {
        this.readingValue = readingValue;
    }

    public Instant getReadingAt() {
        return readingAt;
    }

    public void setReadingAt(Instant readingAt) {
        this.readingAt = readingAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}

