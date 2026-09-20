package com.ubiqedge.billing_software.repository;



import com.ubiqedge.billing_software.entity.WaterMeterReading;
import com.ubiqedge.billing_software.constant.ReadingType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WaterMeterReadingRepository
        extends JpaRepository<WaterMeterReading, UUID> {

    boolean existsByWaterMeterIdAndReadingTypeAndReadingAt(
            UUID waterMeterId,
            ReadingType readingType,
            Instant readingAt
    );

    @Query("""
        SELECT r
        FROM WaterMeterReading r
        WHERE r.waterMeterId = :waterMeterId
          AND r.readingType = com.ubiqedge.billing_software.enums.ReadingType.TOTAL
          AND r.readingAt >= :from
          AND r.readingAt <= :to
        ORDER BY r.readingAt ASC
        """)
    List<WaterMeterReading> findTotalReadingsBetween(
            @Param("waterMeterId") UUID waterMeterId,
            @Param("from") Instant from,
            @Param("to") Instant to
    );

    @Query("""
            SELECT r
            FROM WaterMeterReading r
            WHERE r.waterMeterId = :waterMeterId
              AND r.readingType = :readingType
              AND r.readingAt <= :readingAt
            ORDER BY r.readingAt DESC
            """)
    Optional<WaterMeterReading> findLatestReading(
            @Param("waterMeterId") UUID waterMeterId,
            @Param("readingType") ReadingType readingType,
            @Param("readingAt") Instant readingAt
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
    List<WaterMeterReading> findReadingsBetween(
            @Param("waterMeterId") UUID waterMeterId,
            @Param("readingType") ReadingType readingType,
            @Param("from") Instant from,
            @Param("to") Instant to
    );
}

