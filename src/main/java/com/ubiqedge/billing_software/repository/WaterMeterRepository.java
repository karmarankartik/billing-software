package com.ubiqedge.billing_software.repository;

import com.ubiqedge.billing_software.entity.WaterMeter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WaterMeterRepository extends JpaRepository<WaterMeter, UUID> {

    @Query(value = """
            SELECT *
            FROM water_meters
            WHERE id = :id
              AND deleted_at IS NULL
            """, nativeQuery = true)
    Optional<WaterMeter> findActiveById(@Param("id") UUID id);

    @Query(value = """
            SELECT *
            FROM water_meters
            WHERE deleted_at IS NULL
            ORDER BY created_at DESC
            """, nativeQuery = true)
    List<WaterMeter> findAllActive();

    @Query(value = """
            SELECT COUNT(*)
            FROM water_meters
            WHERE meter_number = :meterNumber
              AND deleted_at IS NULL
            """, nativeQuery = true)
    long countActiveByMeterNumber(
            @Param("meterNumber") String meterNumber
    );

    @Query(value = """
            SELECT COUNT(*)
            FROM water_meters
            WHERE meter_number = :meterNumber
              AND id <> :id
              AND deleted_at IS NULL
            """, nativeQuery = true)
    long countActiveByMeterNumberAndIdNot(
            @Param("meterNumber") String meterNumber,
            @Param("id") UUID id
    );
}