package com.ubiqedge.billing_software.repository;

import com.ubiqedge.billing_software.entity.WaterMeter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WaterMeterRepository extends JpaRepository<WaterMeter, UUID> {

    Optional<WaterMeter> findByIdAndDeletedAtIsNull(UUID id);

    Optional<WaterMeter> findByMeterNumberAndDeletedAtIsNull(String meterNumber);

    boolean existsByMeterNumberAndDeletedAtIsNull(String meterNumber);

    List<WaterMeter> findAllByDeletedAtIsNull();

    @Query(value = """
    SELECT wm.*
    FROM water_meters wm
    INNER JOIN water_meter_assignments wma
        ON wma.water_meter_id = wm.id
    WHERE wma.user_id = :userId
      AND wma.unassigned_at IS NULL
      AND wm.deleted_at IS NULL
    """, nativeQuery = true)
    List<WaterMeter> findActiveMetersByUserId(
            @Param("userId") UUID userId);

    @Query("""
        SELECT COUNT(wm)
        FROM WaterMeter wm
        JOIN WaterMeterAssignment wma
          ON wma.waterMeterId = wm.id
        WHERE wma.userId = :userId
          AND wm.deletedAt IS NULL
        """)
    long countMetersForUser(@Param("userId") UUID userId);


}