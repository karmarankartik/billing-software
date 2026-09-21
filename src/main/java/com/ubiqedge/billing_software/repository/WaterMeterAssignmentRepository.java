package com.ubiqedge.billing_software.repository;

import com.ubiqedge.billing_software.entity.WaterMeterAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WaterMeterAssignmentRepository
        extends JpaRepository<WaterMeterAssignment, UUID> {

    Optional<WaterMeterAssignment> findByActiveAssignmentKey(UUID activeAssignmentKey);

    Optional<WaterMeterAssignment> findByWaterMeterIdAndUnassignedAtIsNull(
            UUID waterMeterId
    );

    @Query(value = """
        SELECT wma.*
        FROM water_meter_assignments wma
        JOIN water_meters wm
          ON wm.id = wma.water_meter_id
        WHERE wma.user_id = :userId
          AND wm.deleted_at IS NULL
          AND wma.assigned_at < :periodEnd
          AND (
                wma.unassigned_at IS NULL
                OR wma.unassigned_at > :periodStart
              )
        ORDER BY wma.water_meter_id, wma.assigned_at
        """, nativeQuery = true)
    List<WaterMeterAssignment> findAssignmentsForUserOverlappingPeriod(
            @Param("userId") UUID userId,
            @Param("periodStart") Instant periodStart,
            @Param("periodEnd") Instant periodEnd
    );


    @Query(value = """
        SELECT wma.*
        FROM water_meter_assignments wma
        JOIN water_meters wm
          ON wm.id = wma.water_meter_id
        WHERE wm.deleted_at IS NULL
          AND wma.assigned_at < :periodEnd
          AND (
                wma.unassigned_at IS NULL
                OR wma.unassigned_at > :periodStart
              )
        ORDER BY wma.water_meter_id, wma.assigned_at
        """, nativeQuery = true)
    List<WaterMeterAssignment> findAssignmentsOverlappingPeriod(
            @Param("periodStart") Instant periodStart,
            @Param("periodEnd") Instant periodEnd
    );
}