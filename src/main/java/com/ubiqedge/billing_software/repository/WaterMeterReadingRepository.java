/*
package com.ubiqedge.billing_software.repository;

import com.ubiqedge.billing_software.constant.ReadingType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface WaterMeterReadingRepository
        extends JpaRepository<WaterMeterReading, UUID> {

    Optional<WaterMeterReading> findByWaterMeterIdAndIngestionKey(
            UUID waterMeterId,
            String ingestionKey
    );

    Optional<WaterMeterReading> findByWaterMeterIdAndReadingTypeAndReadingAt(
            UUID waterMeterId,
            ReadingType readingType,
            OffsetDateTime readingAt
    );

    @Query("""
        SELECT r
        FROM WaterMeterReading r
        WHERE r.waterMeterId = :waterMeterId
          AND r.readingType = :readingType
          AND r.readingAt <= :readingAt
        ORDER BY r.readingAt DESC
        """)
    Optional<WaterMeterReading> findLatestReadingAtOrBefore(
            @Param("waterMeterId") UUID waterMeterId,
            @Param("readingType") ReadingType readingType,
            @Param("readingAt") OffsetDateTime readingAt
    );

    @Query("""
        SELECT r
        FROM WaterMeterReading r
        WHERE r.waterMeterId = :waterMeterId
          AND r.readingType = :readingType
          AND r.readingAt < :readingAt
        ORDER BY r.readingAt DESC
        """)
    Optional<WaterMeterReading> findLatestReadingBefore(
            @Param("waterMeterId") UUID waterMeterId,
            @Param("readingType") ReadingType readingType,
            @Param("readingAt") OffsetDateTime readingAt
    );

    @Query("""
        SELECT r
        FROM WaterMeterReading r
        WHERE r.waterMeterId = :waterMeterId
          AND r.readingType = :readingType
          AND r.readingAt >= :from
          AND r.readingAt < :to
        ORDER BY r.readingAt ASC
        """)
    java.util.List<WaterMeterReading> findReadingsBetween(
            @Param("waterMeterId") UUID waterMeterId,
            @Param("readingType") ReadingType readingType,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to
    );
}*/
