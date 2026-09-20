package com.ubiqedge.billing_software.repository;



import com.ubiqedge.billing_software.entity.WaterMeterAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WaterMeterAssignmentRepository
        extends JpaRepository<WaterMeterAssignment, UUID> {

    @Query(value = """
            SELECT *
            FROM water_meter_assignments
            WHERE water_meter_id = :waterMeterId
              AND unassigned_at IS NULL
              AND active_assignment_key = :waterMeterId
            """, nativeQuery = true)
    Optional<WaterMeterAssignment> findActiveByWaterMeterId(
            @Param("waterMeterId") UUID waterMeterId
    );

    @Query(value = """
            SELECT *
            FROM water_meter_assignments
            WHERE user_id = :userId
              AND unassigned_at IS NULL
            ORDER BY assigned_at DESC
            """, nativeQuery = true)
    List<WaterMeterAssignment> findActiveByUserId(
            @Param("userId") UUID userId
    );

    @Modifying
    @Query(value = """
            UPDATE water_meter_assignments
            SET unassigned_at = :unassignedAt,
                active_assignment_key = NULL
            WHERE water_meter_id = :waterMeterId
              AND unassigned_at IS NULL
            """, nativeQuery = true)
    int unassignActiveMeter(
            @Param("waterMeterId") UUID waterMeterId,
            @Param("unassignedAt") Instant unassignedAt
    );


    @Query(value = """
        SELECT *
        FROM water_meter_assignments
        WHERE water_meter_id = :waterMeterId
          AND assigned_at < :periodEnd
          AND (unassigned_at IS NULL OR unassigned_at > :periodStart)
        ORDER BY assigned_at
        """, nativeQuery = true)
    List<WaterMeterAssignment> findAssignmentsForPeriod(
            @Param("waterMeterId") UUID waterMeterId,
            @Param("periodStart") Instant periodStart,
            @Param("periodEnd") Instant periodEnd
    );


}

