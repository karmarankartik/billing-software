
        package com.ubiqedge.billing_software.repository;

import com.ubiqedge.billing_software.entity.WaterMeterReading;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface WaterMeterReadingRepository
        extends JpaRepository<WaterMeterReading, UUID> {



    @Query(value = """
        SELECT wmr.*
        FROM water_meter_readings wmr
        WHERE wmr.water_meter_id = :waterMeterId
          AND wmr.reading_type = 'TOTAL'
          AND wmr.reading_at <= :readingAt
        ORDER BY wmr.reading_at DESC
        LIMIT 1
        """, nativeQuery = true)
    Optional<WaterMeterReading> findLatestTotalReadingBeforeOrAt(
            @Param("waterMeterId") UUID waterMeterId,
            @Param("readingAt") Instant readingAt
    );


    @Query(value = """
        SELECT *
        FROM water_meter_readings
        WHERE water_meter_id = :waterMeterId
          AND reading_type = 'TOTAL'
          AND reading_at >= :dayStart
          AND reading_at < :nextDayStart
        ORDER BY reading_at DESC
        LIMIT 1
        """, nativeQuery = true)
    Optional<WaterMeterReading> findLatestTotalReadingForDay(
            @Param("waterMeterId") UUID waterMeterId,
            @Param("dayStart") Instant dayStart,
            @Param("nextDayStart") Instant nextDayStart
    );




}
