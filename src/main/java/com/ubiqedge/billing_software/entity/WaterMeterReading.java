package com.ubiqedge.billing_software.entity;


import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "water_meter_readings")
public class WaterMeterReading {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "water_meter_id", nullable = false)
    private UUID waterMeterId;

    @Column(name = "reading_type", nullable = false, length = 20)
    private String readingType;

    @Column(name = "reading_value", nullable = false, precision = 19, scale = 6)
    private BigDecimal readingValue;

    @Column(name = "reading_at", nullable = false)
    private Instant readingAt;

    @Column(name = "ingestion_key", nullable = false, length = 255)
    private String ingestionKey;

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

    public String getReadingType() {
        return readingType;
    }

    public void setReadingType(String readingType) {
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

    public String getIngestionKey() {
        return ingestionKey;
    }

    public void setIngestionKey(String ingestionKey) {
        this.ingestionKey = ingestionKey;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
